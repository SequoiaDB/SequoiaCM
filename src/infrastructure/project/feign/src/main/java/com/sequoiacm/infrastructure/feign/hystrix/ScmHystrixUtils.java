package com.sequoiacm.infrastructure.feign.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;

public class ScmHystrixUtils {
    public static Exception transferHystrixException(Exception e, HystrixCommand<?> hystrixCommand,
            String target) {
        // 超过信号量大小被拒绝
        if (hystrixCommand.isResponseSemaphoreRejected()) {
            int maxConcurrentRequests = hystrixCommand.getProperties()
                    .executionIsolationSemaphoreMaxConcurrentRequests().get();
            return new ScmHystrixException(
                    formatSemaphoreRejectedMsg(target, maxConcurrentRequests), e);
        }
        // 超过线程池队列大小被拒绝
        else if (hystrixCommand.isResponseThreadPoolRejected()) {
            Integer queueSize = HystrixThreadPoolMetrics
                    .getInstance(hystrixCommand.getThreadPoolKey()).getProperties().maxQueueSize()
                    .get();
            return new ScmHystrixException(formatThreadPoolRejectedMsg(target, queueSize), e);
        }
        // 被熔断器拒绝
        else if (hystrixCommand.isResponseShortCircuited()) {
            return new ScmHystrixException(formatShortCircuitedMsg(target), e);
        }
        if (e.getCause() != null) {
            return new Exception(e.getCause().getMessage(), e);
        }
        return e;
    }

    public static String formatShortCircuitedMsg(String target) {
        String msg = String.format(
                "%s[%s] calling %s was rejected by Circuit-Breaker because of a large number of failures.",
                ScmFeignClient.localService, ScmFeignClient.localHostPort, target);
        return msg;
    }

    public static String formatThreadPoolRejectedMsg(String target, int queueSize) {
        String msg = String.format(
                "The concurrency of %s[%s] calling %s exceeds the Hystrix threadPool queueSize:%d, You can adjust the Hystrix threadPool configuration.",
                ScmFeignClient.localService, ScmFeignClient.localHostPort, target, queueSize);
        return msg;
    }

    public static String formatSemaphoreRejectedMsg(String target, int maxConcurrentRequests) {
        String msg = String.format(
                "The concurrency of %s[%s] calling %s exceeds the maximum number limit:%d, You can adjust the Hystrix semaphore configuration.",
                ScmFeignClient.localService, ScmFeignClient.localHostPort, target,
                maxConcurrentRequests);
        return msg;
    }
}
