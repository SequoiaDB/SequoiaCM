package com.sequoiacm.infrastructure.common.annotation;

import java.lang.annotation.*;

/**
 * 用于统计方法执行耗时 使用该注解的方法会被自动统计执行耗时并记录到 SlowLogContext 中
 */
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {

    /**
     * 操作的名称，不填写时默认使用方法名
     * @return
     */
    String operation() default "";

    /**
     * 添加额外信息，在日志输出时会输出该该额外信息
     * @return
     */
    SlowLogExtra[] extras() default {};

    /**
     * 嵌套调用时是否忽略对内部调用方法执行耗时的计算，只统计当前方法的执行耗时。例如下面的例子中，methodA 在内部调用 methodB:<br/>
     * 当 ignoreNestedCall == true，统计的结果为：[methodA=2000]  <br/>
     * 当 ignoreNestedCall == false，统计的结果为：[methodA=2000，methodA.methodB=1000]  <br/>
     * 在出现方法的递归调用时，建议将该值设为 true
     * <pre> @SlowLog(ignoreNestedCall = true)    </pre>
     * <pre> public void methodA(){          </pre>
     * <pre>    Thread.sleep(1000)           </pre>
     * <pre>    methodB();                   </pre>
     * <pre> }                               </pre>
     *
     * <pre> @SlowLog                        </pre>
     * <pre> public void methodB(){          </pre>
     * <pre>    Thread.sleep(1000)           </pre>
     * <pre> }                               </pre>
     *
     * @return
     */
    boolean ignoreNestedCall() default false;

}