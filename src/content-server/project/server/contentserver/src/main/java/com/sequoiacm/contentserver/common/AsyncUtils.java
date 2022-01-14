package com.sequoiacm.contentserver.common;

import com.sequoiacm.contentserver.config.AsyncConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class AsyncUtils {


    private static ThreadPoolExecutor threadPoolExecutor;

    private static boolean enabled = true;

    @Autowired
    public AsyncUtils(AsyncConfig asyncConfig) {
        AsyncUtils.threadPoolExecutor = new ThreadPoolExecutor(asyncConfig.getCorePoolSize(),
                asyncConfig.getMaxPoolSize(), asyncConfig.getThreadKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(asyncConfig.getBlockingQueueSize()),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        AsyncUtils.enabled = asyncConfig.isEnabled();
    }

    public static void execute(Runnable command) {
        if (enabled && threadPoolExecutor != null) {
            threadPoolExecutor.execute(command);
        }
        else {
            command.run();
        }

    }

}
