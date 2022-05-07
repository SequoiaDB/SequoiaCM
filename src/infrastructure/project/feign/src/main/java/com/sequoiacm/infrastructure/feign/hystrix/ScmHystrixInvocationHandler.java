package com.sequoiacm.infrastructure.feign.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.HystrixCommand.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.slowlog.SlowLogManager;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static feign.Util.checkNotNull;

/**
 * 参考FeignInvocationHandler重写，主要是为了对业务异常的熔断进行特殊处理
 * 
 * @see feign.ReflectiveFeign.FeignInvocationHandler
 * @see feign.hystrix.HystrixInvocationHandler
 */
public class ScmHystrixInvocationHandler implements InvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(ScmHystrixInvocationHandler.class);

    private final Target<?> target;
    protected final Map<Method, MethodHandler> dispatch;
    private final String groupKey;
    private final String commandKey;

    public ScmHystrixInvocationHandler(Target<?> target, Map<Method, MethodHandler> dispatch,
            String groupKey, String commandKey) {
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch");
        this.groupKey = checkNotNull(groupKey, "groupKey").toLowerCase();
        this.commandKey = checkNotNull(commandKey, "commandKey").toLowerCase();
    }

    @Override
    @SlowLog(operation = "feign")
    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable {

        // early exit if the invoked method is from java.lang.Object
        // code is the same as ReflectiveFeign.FeignInvocationHandler
        if ("equals".equals(method.getName())) {
            try {
                Object otherHandler = args.length > 0 && args[0] != null
                        ? Proxy.getInvocationHandler(args[0])
                        : null;
                return equals(otherHandler);
            }
            catch (IllegalArgumentException e) {
                return false;
            }
        }
        else if ("hashCode".equals(method.getName())) {
            return hashCode();
        }
        else if ("toString".equals(method.getName())) {
            return toString();
        }

        /**
         * 当Spring容器还未完全启动时，使用普通调用方式，主要是为了防止出现两个问题:
         * 1. 此时，Hystrix的配置还未完成加载，无法使用到正确的配置
         * 2. 采用线程池作为隔离方式时可能会出现死锁的问题
         */
        if (!ScmFeignClient.isApplicationStarted) {
            try {
                SlowLogManager.getCurrentContext().beginOperation(method.getName());
                return dispatch.get(method).invoke(args);
            }
            catch (Exception e) {
                if (e instanceof ScmHystrixIgnoreException) {
                    throw e.getCause();
                }
                throw e;
            }
            finally {
                SlowLogManager.getCurrentContext().endOperation();
            }
        }

        HystrixCommand<ScmHystrixExecuteResultWrapper> hystrixCommand = createCommand(method, args);
        try {
            SlowLogManager.getCurrentContext().beginOperation(method.getName());
            ScmHystrixExecuteResultWrapper resultWrapper = hystrixCommand.execute();
            if (resultWrapper.isExecuteFailed()) {
                throw resultWrapper.getError();
            }
            return resultWrapper.getResult();
        }
        catch (Exception e) {
            throw getRealException(e, hystrixCommand);
        }
        finally {
            SlowLogManager.getCurrentContext().endOperation();
        }

    }

    private Throwable getRealException(Exception e, HystrixCommand<?> hystrixCommand) {
        if (e instanceof HystrixRuntimeException) {
            return ScmHystrixUtils.transferHystrixException(e, hystrixCommand, commandKey);
        }
        else {
            return e;
        }
    }

    protected HystrixCommand<ScmHystrixExecuteResultWrapper> createCommand(final Method method,
            final Object[] args) {
        HystrixCommand<ScmHystrixExecuteResultWrapper> hystrixCommand = new HystrixCommand<ScmHystrixExecuteResultWrapper>(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                        .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))) {
            @Override
            protected ScmHystrixExecuteResultWrapper run() throws Exception {
                try {
                    Object result = ScmHystrixInvocationHandler.this.dispatch.get(method)
                            .invoke(args);
                    return new ScmHystrixExecuteResultWrapper(result);
                }
                catch (Exception e) {
                    if (e instanceof ScmHystrixIgnoreException) {
                        // ScmHystrixIgnoreException 代表是一个被Hystrix忽略的异常，不往外抛，否则会被Hystrix算作异常请求。
                        return new ScmHystrixExecuteResultWrapper(e.getCause());
                    }
                    else {
                        throw e;
                    }
                }
                catch (Throwable t) {
                    throw (Error) t;
                }
            }
        };
        return hystrixCommand;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScmHystrixInvocationHandler) {
            ScmHystrixInvocationHandler other = (ScmHystrixInvocationHandler) obj;
            return target.equals(other.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return target.toString();
    }

    static class ScmHystrixExecuteResultWrapper {

        private Object result;
        private boolean executeFailed = false;
        private Throwable error;

        public ScmHystrixExecuteResultWrapper(Object result) {
            this.result = result;
        }

        public ScmHystrixExecuteResultWrapper(Throwable error) {
            this.error = error;
            this.executeFailed = true;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.executeFailed = false;
            this.result = result;
        }

        public boolean isExecuteFailed() {
            return executeFailed;
        }

        public Throwable getError() {
            return error;
        }

        public void setError(Exception error) {
            this.executeFailed = true;
            this.error = error;
        }
    }

}
