package com.sequoiacm.common.memorypool;

import com.sequoiacm.infrastructure.common.ApplicationConfig;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ScmMemoryPool implements IMemoryPool {
    private static final Logger logger = LoggerFactory.getLogger(ScmMemoryPool.class);
    private HashMap<Integer, ConcurrentLinkedQueue<byte[]>> idlePool = new HashMap<Integer, ConcurrentLinkedQueue<byte[]>>();
    private final HashMap<String, Integer> defaultOptionMap = new HashMap<String, Integer>();

    private volatile boolean hasClosed = false;
    private final AtomicInteger jvmAssignmentCount = new AtomicInteger(0);
    private final AtomicInteger poolHitCount = new AtomicInteger(0);
    private final AtomicInteger getBytesCount = new AtomicInteger(0);
    private volatile long lastJvmAssignmentTime = 0L;

    // 在定时任务缩容时，会对比该值与池子的10%，取两者最大值进行缩容
    private int maxIdlePoolSize = 0;
    // 如果归还内存时，发现池子的大小已经大于该值，就直接释放
    private int maxPoolSize = 0;
    // 清理定时任务的周期，但是是秒(s)
    private int cleanInterval = 0;

    private ApplicationConfig applicationConfig;

    private ScmTimer cleanTimer = null;
    private ScmTimer printTimer = null;

    public ScmMemoryPool() throws Exception {
        defaultOptionMap.put(ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_CLEANINTERVAL,
                ScmMemoryPoolDefine.DEFAULT_CLEANINTERVAL);
        defaultOptionMap.put(ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_MAXPOOLSIZE,
                ScmMemoryPoolDefine.DEFAULT_MAXPOOLSIZE);
        defaultOptionMap.put(ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_MAXIDLESIZE,
                ScmMemoryPoolDefine.DEFAULT_MAXIDLESIZE);

        this.applicationConfig = ApplicationConfig.getInstance();

        this.cleanInterval = getOptionValue(ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_CLEANINTERVAL);
        this.maxPoolSize = getOptionValue(ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_MAXPOOLSIZE);
        this.maxIdlePoolSize = getOptionValue(ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_MAXIDLESIZE);

        if (maxPoolSize < maxIdlePoolSize) {
            logger.warn(
                    "maxPoolSize need to be greater than maxIdleSize, maxPoolSize={}, maxIdleSize={}, "
                            + "set to default value: maxPoolSize={}, maxIdleSize={}",
                    maxPoolSize, maxIdlePoolSize, ScmMemoryPoolDefine.DEFAULT_MAXPOOLSIZE,
                    ScmMemoryPoolDefine.DEFAULT_MAXIDLESIZE);
            this.maxPoolSize = ScmMemoryPoolDefine.DEFAULT_MAXPOOLSIZE;
            this.maxIdlePoolSize = ScmMemoryPoolDefine.DEFAULT_MAXIDLESIZE;
        }
        idlePool.put(1024 * 1024, new ConcurrentLinkedQueue<byte[]>());
        idlePool.put(5 * 1024 * 1024, new ConcurrentLinkedQueue<byte[]>());
        idlePool.put(255 * 1024, new ConcurrentLinkedQueue<byte[]>());
        idlePool.put(64 * 1024, new ConcurrentLinkedQueue<byte[]>());
        beginCleanTask();
        beginPrintTask();
    }

    private int getOptionValue(String option) {
        String optionValue = applicationConfig.getConfig(option);
        try {
            int value = 0;
            if (optionValue != null) {
                value = Integer.parseInt(optionValue);
            }
            if (optionValue == null || value <= 0) {
                value = defaultOptionMap.get(option);
                logger.warn("{} value is null or invalid, value:{}, set to default value:{}",
                        option, optionValue, value);
            }
            return value;
        }
        catch (Exception e) {
            logger.error("Failed to parse String to int,option={},value={},error={}", option,
                    optionValue, e);
            throw new IllegalArgumentException(
                    "Failed to parse String to int,option=" + option + ",value=" + optionValue, e);
        }
    }

    @Override
    public byte[] getBytes(int size) {
        if (hasClosed) {
            logger.error("Memory pool has closed, byte array size={}", size);
            throw new IllegalStateException("Memory pool has closed, byte array size=" + size);
        }
        ConcurrentLinkedQueue<byte[]> queue = idlePool.get(size);
        if (queue == null) {
            logger.error("The byte array doesn't belong to the memory pool, byte array size={}", size);
            throw new IllegalArgumentException(
                    "The byte array doesn't belong to the memory pool, byte array size=" + size);
        }
        // 统计获取字节数组的总数
        getBytesCount.incrementAndGet();
        byte[] result = queue.poll();
        if (result == null) {
            result = new byte[size];
            lastJvmAssignmentTime = System.currentTimeMillis();
            jvmAssignmentCount.incrementAndGet();
            return result;
        }
        poolHitCount.incrementAndGet();
        return result;
    }

    @Override
    public void releaseBytes(byte[] bytes) {
        if (hasClosed) {
            logger.warn("Memory pool has closed, byte array size={}", bytes.length);
            return;
        }
        ConcurrentLinkedQueue<byte[]> queue = idlePool.get(bytes.length);
        if (queue == null) {
            logger.warn("The byte array doesn't belong to the memory pool, byte array size={}",
                    bytes.length);
            return;
        }
        if (queue.size() < maxPoolSize) {
            queue.offer(bytes);
        }
    }

    private void beginCleanTask() {
        this.cleanTimer = ScmTimerFactory.createScmTimer();
        logger.info("Start clean timer task");
        ScmTimerTask task = new ScmTimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis()
                        - lastJvmAssignmentTime > ScmMemoryPoolDefine.CLEAN_TASK_TRIGGER_INTERVAL
                                * 60 * 1000L) {
                    // 计算清理因子
                    for (Map.Entry<Integer, ConcurrentLinkedQueue<byte[]>> entry : idlePool
                            .entrySet()) {
                        ConcurrentLinkedQueue<byte[]> queue = entry.getValue();
                        int cleanNum = Math.max(
                                (int) (queue.size() * ScmMemoryPoolDefine.CLEAN_FACTOR),
                                ScmMemoryPoolDefine.LOWEST_CLEAN_NUM);
                        int alreadyClean = 0;
                        logger.debug("Current queue is {}, the num of byte array to clean is {}",
                                entry.getKey(), cleanNum);
                        while (cleanNum > 0 && queue.size() > maxIdlePoolSize) {
                            byte[] releaseByte = queue.poll();
                            // 如果队列为空，那么就直接退出此次定时任务
                            if (releaseByte == null) {
                                break;
                            }
                            cleanNum--;
                            alreadyClean++;
                        }
                        logger.debug("Current queue is {}, the num of byte array cleaned is {}",
                                entry.getKey(), alreadyClean);
                    }
                }
            }
        };
        cleanTimer.schedule(task, ScmMemoryPoolDefine.CLEAN_TASK_DELAY * 1000L,
                cleanInterval * 1000L);
    }

    private void beginPrintTask() {
        this.printTimer = ScmTimerFactory.createScmTimer();
        logger.info("Start print timer task");
        ScmTimerTask task = new ScmTimerTask() {
            @Override
            public void run() {
                if (logger.isDebugEnabled()) {
                    printDetail();
                }
            }
        };
        printTimer.schedule(task, 0, ScmMemoryPoolDefine.PRINT_TASK_INTERVAL * 1000L);
    }

    private void printDetail() {
        int poolHit = poolHitCount.intValue();
        int jvmHit = jvmAssignmentCount.intValue();
        int getBytes = getBytesCount.intValue();
        float proportion = getBytes == 0 ? 0 : poolHit / (float) getBytes;
        logger.debug("The times hitting memory pool:{}", poolHit);
        logger.debug("The times assigning jvm:{}", jvmHit);
        logger.debug("The times getting byte array:{}", getBytes);
        logger.debug("Hit rate:{}", proportion);
    }

    @Override
    public void close() {
        if (hasClosed) {
            return;
        }
        if (cleanTimer != null) {
            logger.info("Cancel clean timer task");
            cleanTimer.cancel();
        }
        if (printTimer != null) {
            logger.info("Cancel print timer task");
            printTimer.cancel();
        }
        idlePool = null;
        hasClosed = true;
        printDetail();
    }
}
