package com.sequoiacm.fulltext.server.sch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

@Component
public class IdxThreadPool {
    private ThreadPoolExecutor taskMgr;

    public IdxThreadPool(IdxThreadPoolConfig conf) {
        taskMgr = new ThreadPoolExecutor(conf.getCorePoolSize(), conf.getMaxPoolSize(),
                conf.getKeepAliveTime(), TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(conf.getBlockingQueueSize()),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void submit(Runnable t) {
        taskMgr.submit(t);
    }

    @PreDestroy
    public void destory() {
        taskMgr.shutdownNow();
    }
}
