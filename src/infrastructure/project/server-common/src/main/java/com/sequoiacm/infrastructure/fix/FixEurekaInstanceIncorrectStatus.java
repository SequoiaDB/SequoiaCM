package com.sequoiacm.infrastructure.fix;

import com.netflix.discovery.converters.EurekaJacksonCodec;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * 启动成功节点，在注册中心中状态显示为 STARTING http://jira.web:8080/browse/SEQUOIACM-890
 */
public class FixEurekaInstanceIncorrectStatus {

    private static FixEurekaInstanceIncorrectStatus INSTANCE = null;

    private static final String CLASS_NAME = "com.netflix.discovery.converters.EurekaJacksonCodec$InstanceInfoSerializer";
    private static final String METHOD_NAME = "serialize";

    private boolean isFixed = false;

    private FixEurekaInstanceIncorrectStatus() {
    }

    /**
     * 修改 Eureka 序列化 InstanceInfo 的流程，将序列化 lastDirtTimestamp 字段的时机提前
     * 
     * @see EurekaJacksonCodec.InstanceInfoSerializer#serialize(com.netflix.appinfo.InstanceInfo,
     *      com.fasterxml.jackson.core.JsonGenerator,
     *      com.fasterxml.jackson.databind.SerializerProvider)
     */
    public synchronized void fix() {
        if (isFixed) {
            return;
        }
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        CtClass ctClass = null;
        CtMethod ctMethod = null;
        try {
            ctClass = pool.get(CLASS_NAME);
            ctMethod = ctClass.getDeclaredMethod(METHOD_NAME);
        }
        catch (NotFoundException ignored) {
            return;
        }
        try {
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    super.edit(m);
                    if (m.getMethodName().equals("autoMarshalEligible")) {
                        m.replace(";");
                    }
                }
            });
            ctMethod.insertAt(325, "autoMarshalEligible(info, jgen);");
            ctClass.toClass();
            ctClass.detach();
        }
        catch (Exception e) {
            throw new RuntimeException("failed to fix: EurekaInstanceIncorrectStatus", e);
        }
        isFixed = true;

    }

    public static FixEurekaInstanceIncorrectStatus getInstance() {
        if (INSTANCE == null) {
            synchronized (FixEurekaInstanceIncorrectStatus.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FixEurekaInstanceIncorrectStatus();
                }
            }
        }
        return INSTANCE;
    }
}
