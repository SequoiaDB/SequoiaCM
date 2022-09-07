package com.sequoiacm.infrastructure.lock.curator;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorZKCleaner {
    private static int printCount = 10000;
    private long innerCounter = 0;
    private static final Logger logger = LoggerFactory.getLogger(CuratorZKCleaner.class);
    // clean task is single thread, have no need lock
    private static volatile CuratorZKCleaner INSTANCE = null;

    private final ThreadPoolExecutor cleanThreadPool;
    private final ErrorLogDecision errorLogDecision;
    private final CuratorFramework client;
    private volatile boolean closed = false;

    private CuratorZKCleaner(CuratorFramework curatorFramework, int coreCleanThreads,
            int maxCleanThreads,
            int queueSize) {
        this.client = curatorFramework;
        this.cleanThreadPool = new ThreadPoolExecutor(coreCleanThreads, maxCleanThreads, 60,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        this.errorLogDecision = new ErrorLogDecision(10 * 1000);
        logger.info("init CuratorZKCleaner,coreCleanThreads={}, maxCleanThreads={}, queueSize={}",
                coreCleanThreads, maxCleanThreads,
                queueSize);
    }


    public static CuratorZKCleaner getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("CuratorZKCleaner is not initialized");
        }
        return INSTANCE;
    }

    public static boolean isInitialized() {
        return INSTANCE != null;
    }

    public static void init(CuratorFramework curatorFramework, int coreCleanThreads,
            int maxCleanThreads, int queueSize) {
        if (INSTANCE == null) {
            synchronized (CuratorZKCleaner.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CuratorZKCleaner(curatorFramework, coreCleanThreads,
                            maxCleanThreads, queueSize);
                }
            }
        }
    }

    public void putPath(String path) {
        if (closed || path == null || path.isEmpty()) {
            return;
        }
        cleanThreadPool.execute(new DeletePathTask(path));
    }

    private class DeletePathTask implements Runnable {

        private String path;

        public DeletePathTask(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            deletePathSilence(path);
        }

        @Override
        public String toString() {
            return "DeletePathTask{" + "path='" + path + '\'' + '}';
        }
    }

    private void deletePathSilence(String path) {
        try {
            client.delete().forPath(path);
        }
        catch (KeeperException.NoNodeException | KeeperException.NotEmptyException ignored) {
            // NoNodeException: 节点已经被删除了，直接忽略
            // NotEmptyException: 节点在删除的过程中又进行了加锁，也忽略
        }
        catch (Exception e) {
            ErrorLogDecision.DecideResult result = errorLogDecision.decide(e);
            if (result.isShouldLog()) {
                if (result.getIgnoreCount() <= 0) {
                    logger.warn("failed to delete zk path:{}", path, e);
                }
                else {
                    logger.warn("failed to delete zk path:{}, ignore exception count:{}", path,
                            result.getIgnoreCount(), e);
                }
            }
        }
    }

    public void close() {
        if (closed) {
            return;
        }
        this.closed = true;
        List<Runnable> tasks = this.cleanThreadPool.shutdownNow();
        logger.info("CuratorZKCleaner is closed, remain path count:{}", tasks.size());
        try {
            this.cleanThreadPool.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            logger.warn("failed to await cleanThreadPool termination", e);
        }
    }

    private boolean checkDeleteNode(CuratorFramework client, String path, long maxResidualTime,
                                    Stat ckStat) throws Exception {
        if (ckStat.getEphemeralOwner() == 0) {
            long zkNodeMtimeMS = ckStat.getMtime();
            long sysTimeMS = System.currentTimeMillis();
            long nodeLifeTimeMS = sysTimeMS - zkNodeMtimeMS;
            if (nodeLifeTimeMS > maxResidualTime) {
                try {
                    CuratorZKBase.deleteNode(client, path);
                    innerCounter++;
                    if (innerCounter % printCount == 0) {
                        logger.info("zk cleaner delete 1w node count");
                    }
                    return true;
                }
                catch (NoNodeException e) {
                    return false;
                }
                catch (NotEmptyException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public long clearResidualNode(CuratorFramework client, String path, long maxResidualTime)
            throws Exception {
        long count = 0;
        Stat ckStat = CuratorZKBase.exists(client, path);
        if (ckStat == null) {
            return count;
        }
        if (ckStat.getNumChildren() == 0) {
            if (checkDeleteNode(client, path, maxResidualTime, ckStat)) {
                count++;
            }
        }
        else {
            List<String> childNodes = null;
            try {
                childNodes = CuratorZKBase.getChildren(client, path);
            }
            catch (NoNodeException e) {
                return count;
            }
            for (String childNode : childNodes) {
                String childParentPath = path + CuratorLockProperty.LOCK_PATH_SEPERATOR + childNode;
                count += clearResidualNode(client, childParentPath, maxResidualTime);
            }
            if (checkDeleteNode(client, path, maxResidualTime, ckStat)) {
                count++;
            }
        }
        return count;
    }
}

class ErrorLogDecision {

    private long lastExceptionTime;

    private Exception lastException;
    private Class<? extends Throwable> lastRootExceptionType;

    private final int logInterval;

    private int ignoreCount;

    public ErrorLogDecision(int logInterval) {
        this.logInterval = logInterval;
    }

    public synchronized DecideResult decide(Exception e) {
        DecideResult result = new DecideResult();
        // 打印日志间隔超过 logInterval，或与上次异常类型不同，都需要打印
        Class<? extends Throwable> rootExceptionType = findRootExceptionType(e);
        if (((System.currentTimeMillis() - lastExceptionTime) >= logInterval)
                || (lastRootExceptionType != null && lastRootExceptionType != rootExceptionType)) {
            result.setShouldLog(true);
            result.setIgnoreCount(ignoreCount);
            ignoreCount = 0;
        }
        else {
            ignoreCount++;
        }
        lastException = e;
        lastRootExceptionType = rootExceptionType;
        lastExceptionTime = System.currentTimeMillis();
        return result;
    }

    private Class<? extends Throwable> findRootExceptionType(Exception e) {
        int maxFindDepth = 20;
        Throwable root = e;
        while (maxFindDepth-- > 0) {
            if (root.getCause() == null) {
                return root.getClass();
            }
            else {
                root = root.getCause();
            }
        }
        return root.getClass();
    }

    static class DecideResult {
        private boolean shouldLog;
        private int ignoreCount;

        public void setShouldLog(boolean shouldLog) {
            this.shouldLog = shouldLog;
        }

        public void setIgnoreCount(int ignoreCount) {
            this.ignoreCount = ignoreCount;
        }

        public boolean isShouldLog() {
            return shouldLog;
        }

        public int getIgnoreCount() {
            return ignoreCount;
        }
    }

}
