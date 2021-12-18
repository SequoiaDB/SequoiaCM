package com.sequoiacm.infrastructure.feign.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.springframework.stereotype.Component;

@Component
public class ScmHystrixExecutor {

    private static ScmHystrixConfig scmHystrixConfig = null;

    public ScmHystrixExecutor(ScmHystrixConfig scmHystrixConfig) {
        ScmHystrixExecutor.scmHystrixConfig = scmHystrixConfig;
    }

    public static <T> T execute(String groupKey, String commandKey, final Runnable<T> run)
            throws Exception {
        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
        return execute(setter, run);
    }

    private static <T> T execute(HystrixCommand.Setter setter, final Runnable<T> run)
            throws Exception {
        if (scmHystrixConfig == null || !scmHystrixConfig.isEnabled()) {
            return run.run();
        }
        HystrixCommand<T> hystrixCommand = new HystrixCommand<T>(setter) {
            @Override
            protected T run() throws Exception {
                return run.run();
            }
        };
        try {
            return hystrixCommand.execute();
        }
        catch (HystrixRuntimeException hystrixRuntimeException) {
            throw ScmHystrixUtils.transferHystrixException(hystrixRuntimeException, hystrixCommand,
                    hystrixCommand.getCommandKey().name());
        }
    }

    public interface Runnable<T> {
        T run() throws Exception;
    }

}
