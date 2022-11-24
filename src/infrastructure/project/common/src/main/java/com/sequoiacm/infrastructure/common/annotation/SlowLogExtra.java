package com.sequoiacm.infrastructure.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLogExtra {

    /**
     * 额外信息的名称
     * 
     * @return
     */
    String name();

    /**
     * 额外信息内容
     * 
     * @return
     */
    String data();

    /**
     * 额外信息内容的类型，支持字面量类型的内容：TEXT，也支持使用 @SlowLog
     * 注解所在的方法的入参变量或所在对象的成员变量的值作为内容：VARIABLE
     * 
     * @return
     */
    String dataType() default SlowLogExtraType.VARIABLE;

}
