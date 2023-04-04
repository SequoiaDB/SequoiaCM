package com.sequoiacm.diagnose.task;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DataCheckRunner {
    private ThreadPoolExecutor threadPool;
    private static final int MAX_POOL_TASK_COUNT = 1000;

    public DataCheckRunner(int workCount) {
        this.threadPool = new ThreadPoolExecutor(workCount, workCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(MAX_POOL_TASK_COUNT),
                new BlockWhenQueueFullHandler());
    }

    public void submit(Runnable task) {
        threadPool.execute(task);
    }

    public void close() {
        if (null != threadPool) {
            threadPool.shutdown();
        }
    }
}

class BlockWhenQueueFullHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            executor.getQueue().put(new FutureTask(r, null));
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
