package com.sequoiacm.mq.server.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "scm.mq.feedbackExecutor")
public class MsgArriveNotifierExecutorConfig {
    private int corePoolSize = 5;
    private int maxPoolSize = 10;
    private int pendingQueueSize = 50;

    @Bean("msgArriveNotifierExecutor")
    public ThreadPoolTaskExecutor msgArriveNotifierExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setKeepAliveSeconds(15);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setThreadNamePrefix("msgArriveNotifierAsync-");
        executor.setQueueCapacity(pendingQueueSize);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
