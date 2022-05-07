package com.sequoiacm.infrastructure.feign;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.slowlog.SlowLogManager;
import feign.InvocationHandlerFactory;
import feign.Target;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static feign.Util.checkNotNull;

/**
 * 参考 feign.ReflectiveFeign.FeignInvocationHandler ，植入慢操作统计逻辑
 */
public class ScmFeignInvocationHandler implements InvocationHandler {

    private final Target target;
    private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

    ScmFeignInvocationHandler(Target target,
            Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch for %s", target);
    }

    @Override
    @SlowLog(operation = "feign")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        try {
            SlowLogManager.getCurrentContext().beginOperation(method.getName());
            return dispatch.get(method).invoke(args);
        }
        finally {
            SlowLogManager.getCurrentContext().endOperation();
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScmFeignInvocationHandler) {
            ScmFeignInvocationHandler other = (ScmFeignInvocationHandler) obj;
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
}
