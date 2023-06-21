package com.sequoiacm.infrastructure.common;

import com.sequoiacm.common.CommonDefine;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对 Controller 层某个执行比较耗时方法的连接进行保活，在使用时需要注意：<br>
 * 1. 不能用在通过响应头返回数据的接口上，因为会造成响应头丢失。 <br>
 * 2. 使用连接保活的接口在触发异常时，返回的状态可能还是200，因此正常响应时可能也会含有异常消息，作为接口的调用方，需要手动从响应体中解析异常消息。<br>
 * 3. 如果接口之前没有返回值，使用连接保活后可能会返回空格内容或异常响应体，需要注意处理。
 * @see EnableRequestKeepAlive
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KeepAlive {

    // 如果请求中携带了该参数，并且该参数的值为 true，则进行连接保活，否则不保活
    String keepAliveParameterName() default CommonDefine.RestArg.KEEP_ALIVE;
}
