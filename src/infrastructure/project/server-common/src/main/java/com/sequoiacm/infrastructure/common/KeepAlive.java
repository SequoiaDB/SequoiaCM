package com.sequoiacm.infrastructure.common;

import com.sequoiacm.common.CommonDefine;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对 Controller 层某个执行比较耗时方法的连接进行保活，在使用时需要注意： 1. 不能用在通过响应头返回数据的接口上。 2. 使用连接保活的接口在触发异常时，返回的状态可能还是
 * 200，因此正常响应时可能也含有异常消息，作为接口的调用方，需要手动从响应体中解析异常消息
 * @see EnableRequestKeepAlive
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KeepAlive {

    // 如果请求中携带了该参数，并且该参数的值为 true，则进行连接保活，否则不保活
    String keepAliveParameterName() default CommonDefine.RestArg.KEEP_ALIVE;
}
