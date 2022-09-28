package com.sequoiacm.contentserver.job;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sequoiacm.contentserver.config.ScmJobManagerConfig;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.infrastructure.common.thread.ScmThreadFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.ServiceDefine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
// 该注解使动态配置刷新时，立即触发 ScmJobManagerConfig 的 @PostConstruct 方法
@EnableConfigurationProperties(ScmJobManagerConfig.class)
public class ScmJobManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmJobManager.class);

    /**
     * 该线程池适合执行时间较短的任务，如异步文件任务（异步迁移、缓存等），subTask任务（ScmFileMoveSubTask、ScmFileCleanSubTask等）
     */
    private final ThreadPoolExecutor shortTimeTaskThreadPool;

    /**
     * 该线程池适合执行时间较长的任务，如调度任务（ScmTaskCleanFile、ScmTaskMoveFile等）
     */
    private final ThreadPoolExecutor longTimeTaskThreadPool;

    private ScmTimer scmTimer = ScmTimerFactory.createScmTimer(2);
    private ScmLogResourceJob logResourceJob = new ScmLogResourceJob();
    private static volatile ScmJobManager jobManager = null;

    @Autowired
    public ScmJobManager(ScmJobManagerConfig jobManagerConfig) throws ScmServerException {
        this.shortTimeTaskThreadPool = new ThreadPoolExecutor(jobManagerConfig.getCoreSize(),
                jobManagerConfig.getMaxSize(), 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(jobManagerConfig.getQueueSize()),
                new ScmThreadFactory("shortTimeTaskThreadPool"),
                new ShortTimeTaskRejectedHandler(
                        jobManagerConfig.getDefaultTaskWaitingTimeOnReject()));
        logger.info(
                "shortTimeTaskThreadPool initialized, coreSize={}, maxSize={}, queueSize={}, rejectedHandler={}",
                shortTimeTaskThreadPool.getCorePoolSize(),
                shortTimeTaskThreadPool.getMaximumPoolSize(),
                shortTimeTaskThreadPool.getQueue().remainingCapacity(),
                shortTimeTaskThreadPool.getRejectedExecutionHandler());

        this.longTimeTaskThreadPool = new ThreadPoolExecutor(
                jobManagerConfig.getLongTimeThreadPoolCoreSize(),
                jobManagerConfig.getLongTimeThreadPoolMaxSize(), 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(jobManagerConfig.getLongTimeThreadPoolQueueSize()),
                new ScmThreadFactory("longTimeTaskThreadPool"),
                new ThreadPoolExecutor.AbortPolicy());
        logger.info(
                "longTimeTaskThreadPool initialized, coreSize={}, maxSize={}, queueSize={}, rejectedHandler={}",
                longTimeTaskThreadPool.getCorePoolSize(),
                longTimeTaskThreadPool.getMaximumPoolSize(),
                longTimeTaskThreadPool.getQueue().remainingCapacity(),
                longTimeTaskThreadPool.getRejectedExecutionHandler());

        ScmJobManager.jobManager = this;
    }

    public static ScmJobManager getInstance() {
        checkState();
        return jobManager;
    }

    private static void checkState() {
        if (jobManager == null) {
            throw new RuntimeException("ScmJobManager is not initialized");
        }
    }

    @PreDestroy
    public void cancel() {
        try {
            if (scmTimer != null) {
                scmTimer.cancel();
            }
            if (shortTimeTaskThreadPool != null && !shortTimeTaskThreadPool.isShutdown()) {
                shortTimeTaskThreadPool.shutdown();
                shortTimeTaskThreadPool.awaitTermination(10, TimeUnit.SECONDS);
            }

            if (longTimeTaskThreadPool != null && !longTimeTaskThreadPool.isShutdown()) {
                longTimeTaskThreadPool.shutdown();
                longTimeTaskThreadPool.awaitTermination(10, TimeUnit.SECONDS);
            }
        }
        catch (Exception e) {
            logger.warn("failed to cancel ScmJobManager", e);
        }
    }

    public int getCoreThreadSize() {
        checkState();
        return shortTimeTaskThreadPool.getCorePoolSize();
    }

    public int getMaxThreadSize() {
        checkState();
        return shortTimeTaskThreadPool.getMaximumPoolSize();
    }

    public BlockingQueue<Runnable> getTaskQueue() {
        return shortTimeTaskThreadPool.getQueue();
    }

    public void updateThreadPoolConfig(int coreThreadSize, int maxThreadSize) {
        checkState();
        // 需要先设置核心线程，再设置最大线程，否则当新的最大线程比旧的核心线程小时就会报错
        shortTimeTaskThreadPool.setCorePoolSize(coreThreadSize);
        shortTimeTaskThreadPool.setMaximumPoolSize(maxThreadSize);
    }

    public void startLogResourceJob() throws ScmServerException {
        checkState();
        schedule(logResourceJob, ServiceDefine.Job.TRANS_LOG_RESOURCE_DELAY);
    }

    /*
     * @param delay(ms)
     */
    public void schedule(final ScmBackgroundJob task, long delay) throws ScmServerException {
        checkState();
        try {
            ScmTimerTask scmTimerTask = new ScmTimerTask() {
                @Override
                public void run() {
                    task.run();
                }
            };
            long period = task.getPeriod();
            if (period > 0) {
                scmTimer.schedule(scmTimerTask, delay, period);
            }
            else {
                if (delay > 0) {
                    scmTimer.schedule(scmTimerTask, delay);
                }
                else {
                    executeShortTimeTask(scmTimerTask);
                }
            }

            logger.debug("start BackgroundJob success:type=" + task.getType() + ",name="
                    + task.getName() + ",period=" + task.getPeriod());
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "start background job failed:type=" + task.getType()
                    + ",name=" + task.getName(), e);
        }
    }

    public void executeShortTimeTask(Runnable task) throws ScmSystemException {
        checkState();
        try {
            shortTimeTaskThreadPool.execute(task);
        }
        catch (Exception e) {
            if (task instanceof ScmTaskBase) {
                String taskId = ((ScmTaskBase) task).getTaskId();
                throw new ScmSystemException("failed to execute task, taskId=" + taskId, e);
            }
            throw new ScmSystemException("failed to execute task", e);
        }
    }

    public void executeLongTimeTask(Runnable task) throws ScmSystemException {
        checkState();
        try {
            longTimeTaskThreadPool.execute(task);
        }
        catch (Exception e) {
            if (task instanceof ScmTaskBase) {
                String taskId = ((ScmTaskBase) task).getTaskId();
                throw new ScmSystemException("failed to execute task, taskId=" + taskId, e);
            }
            throw new ScmSystemException("failed to execute task", e);
        }

    }

    static class ShortTimeTaskRejectedHandler implements RejectedExecutionHandler {
        private long defaultTaskWaitingTimeOnReject = 1000 * 10;

        public ShortTimeTaskRejectedHandler(long defaultTaskWaitingTimeOnReject) {
            this.defaultTaskWaitingTimeOnReject = defaultTaskWaitingTimeOnReject;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                return;
            }
            if (r instanceof ScmBackgroundJob) {
                ScmBackgroundJob backgroundJob = (ScmBackgroundJob) r;
                if (backgroundJob.retryOnThreadPoolReject()) {
                    try {
                        long waitingTime = backgroundJob.waitingTimeOnReject() > 0
                                ? backgroundJob.waitingTimeOnReject()
                                : defaultTaskWaitingTimeOnReject;
                        boolean success = executor.getQueue().offer(r,
                                waitingTime,
                                TimeUnit.MILLISECONDS);
                        if (!success) {
                            throw new RejectedExecutionException(
                                    "failed to put task to threadPool, threadPool is busy:task="
                                            + r);
                        }
                    }
                    catch (InterruptedException e) {
                        throw new RejectedExecutionException(
                                "failed to put task to threadPool:task=" + r, e);
                    }
                }
            }
            r.run();
        }
    }

    public static void main(String[] args) throws ScmServerException, InterruptedException, UnknownHostException {
        ScmJobManager sjm = ScmJobManager.getInstance();
        ScmBackgroundJob job1 = new ScmBackgroundJob() {
            @Override
            public int getType() {
                return 0;
            }

            @Override
            public long getPeriod() {
                return 2000;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void _run() {
                System.out.println(new Date() + " 1111");
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        ScmBackgroundJob job2 = new ScmBackgroundJob() {
            @Override
            public int getType() {
                return 0;
            }

            @Override
            public long getPeriod() {
                return 4000;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void _run() {
                System.out.println(new Date() + " 2222");
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        sjm.schedule(job1, 1000);
        sjm.schedule(job2, 1000);
        Thread.sleep(5000000);
    }
}
