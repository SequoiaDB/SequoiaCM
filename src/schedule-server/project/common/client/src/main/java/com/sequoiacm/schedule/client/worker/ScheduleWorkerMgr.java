package com.sequoiacm.schedule.client.worker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequoiacm.schedule.client.ScheduleClient;
import com.sequoiacm.schedule.client.config.ScheduleWorkerConfig;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;

@Component
public class ScheduleWorkerMgr {

    @Autowired
    private ScheduleClient schClient;

    private Map<String, ScheduleWorkerBuilder> jobType2WorkerFactory = new ConcurrentHashMap<>();
    private Map<String, Future<?>> schId2WorkerFuture = new ConcurrentHashMap<>();
    private Map<String, ScheduleWorker> schId2Worker = new ConcurrentHashMap<>();

    private ThreadPoolExecutor threadPool;

    private String workerNodeAddr;

    @Autowired
    public ScheduleWorkerMgr(@Value("${server.port}") int serverPort, ScheduleWorkerConfig config)
            throws UnknownHostException {
        String localHost = InetAddress.getLocalHost().getHostName();
        workerNodeAddr = localHost + ":" + serverPort;
        threadPool = new ThreadPoolExecutor(config.getThreadPoolSize() / 2 + 1,
                config.getThreadPoolSize(), 120L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(config.getThreadPoolPendingQueueSize()));
    }

    @PreDestroy
    public void destory() {
        threadPool.shutdown();
    }

    public void registerWorkerFactory(ScheduleWorkerBuilder f) {
        jobType2WorkerFactory.put(f.getJobType(), f);
    }

    public boolean isJobRunning(String schId) {
        return schId2WorkerFuture.containsKey(schId);
    }

    public void stopJob(String schId) throws ScheduleException {
        Future<?> f = schId2WorkerFuture.get(schId);
        if (f == null) {
            return;
        }

        boolean ret = f.cancel(true);
        if (!ret) {
            // 任务已完成或未开始
            schId2Worker.remove(schId);
            schId2WorkerFuture.remove(schId);
            return;
        }

        ScheduleWorker worker = schId2Worker.get(schId);
        if (worker == null) {
            return;
        }

        try {
            worker.waitExit();
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to wait job exit:schId=" + schId, e);
        }
    }

    public synchronized void startJob(String schId, String schName, long startTime, String jobType,
            BSONObject jobData) throws ScheduleException {
        if (schId2WorkerFuture.containsKey(schId)) {
            return;
        }

        ScheduleWorkerBuilder factory = jobType2WorkerFactory.get(jobType);
        if (factory == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "no such job type:" + jobType);
        }

        ScheduleWorker worker = factory.createWorker();
        worker.injectWorkerArg(this, schClient, schId, schName, workerNodeAddr, startTime, jobData);

        Future<?> future = threadPool.submit(worker);

        schId2WorkerFuture.put(schId, future);
        schId2Worker.put(schId, worker);
    }

    synchronized void workerExit(String schId) {
        schId2WorkerFuture.remove(schId);
        schId2Worker.remove(schId);
    }

}
