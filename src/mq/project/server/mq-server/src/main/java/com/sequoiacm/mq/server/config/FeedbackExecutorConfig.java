package com.sequoiacm.mq.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "scm.mq.feedbackExecutor")
public class FeedbackExecutorConfig {
    private int corePoolSize = 5;
    private int maxPoolSize = 10;
    private int pendingQueueSize = 100;

    @Bean("feedbackExecutor")
    public ThreadPoolTaskExecutor feedbackExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setKeepAliveSeconds(15);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("feedbackAsync-");
        executor.setQueueCapacity(pendingQueueSize);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
