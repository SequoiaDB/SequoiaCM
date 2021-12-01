package com.sequoiacm.common.memorypool;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ScmNoPool implements IMemoryPool {
    private static final Logger logger = LoggerFactory.getLogger(ScmNoPool.class);

    private final AtomicInteger getBytesCount = new AtomicInteger(0);
    private ScmTimer printTimer = null;

    public ScmNoPool() {
        beginPrintTask();
    }

    @Override
    public byte[] getBytes(int size) {
        byte[] bytes = new byte[size];
        getBytesCount.incrementAndGet();
        return bytes;
    }

    @Override
    public void releaseBytes(byte[] b) {
    }

    @Override
    public void close() {
        if (printTimer != null) {
            logger.info("Cancel print timer task");
            printTimer.cancel();
        }
        printDetail();
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
        logger.debug("the times assigning jvm:{}", getBytesCount.intValue());
    }
}
