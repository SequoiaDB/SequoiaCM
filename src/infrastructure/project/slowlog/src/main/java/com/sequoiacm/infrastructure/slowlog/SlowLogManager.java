package com.sequoiacm.infrastructure.slowlog;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtraType;
import com.sequoiacm.infrastructure.slowlog.util.ClassMetaInfo;
import com.sequoiacm.infrastructure.slowlog.util.ClassUtils;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class SlowLogManager {

    private static final Logger logger = LoggerFactory.getLogger(SlowLogManager.class);

    public static final SlowLogContext EMPTY_CONTEXT = new NoOpSlowLogContextImpl();

    private static final String BASE_PACKAGE = "com.sequoiacm";

    private static final ThreadLocal<SlowLogContext> slowLogContextLocal = new ThreadLocal<>();

    private static boolean initialized = false;

    private SlowLogManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        logger.info("init SlowLogManager");
        initialized = true;
        String currentClassName = null;
        String currentMethodName = null;
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(
                    new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            List<ClassMetaInfo> classMetaInfoList = ClassUtils.scanClasses(BASE_PACKAGE);
            for (ClassMetaInfo classMetaInfo : classMetaInfoList) {
                if (classMetaInfo.isAnnotation() || classMetaInfo.isInterface()) {
                    continue;
                }
                CtClass ctClass = pool.get(classMetaInfo.getClassName());
                currentClassName = classMetaInfo.getClassName();
                boolean hasChange = false;
                for (CtBehavior ctBehavior : ctClass.getDeclaredBehaviors()) {
                    currentMethodName = ctBehavior.getMethodInfo().getName();
                    SlowLog slowLog = (SlowLog) ctBehavior.getAnnotation(SlowLog.class);
                    if (slowLog != null) {
                        boolean ignoreAfter = slowLog.ignoreNestedCall();
                        String operation = slowLog.operation();
                        if (operation.isEmpty()) {
                            operation = ctBehavior.getMethodInfo().getName();
                        }
                        SlowLogExtra[] slowLogExtras = slowLog.extras();
                        for (SlowLogExtra extra : slowLogExtras) {
                            String name = "\"" + extra.name() + "\"";
                            String data = extra.data();
                            if (SlowLogExtraType.TEXT.equals(extra.dataType())) {
                                data = "\"" + data + "\"";
                            }
                            ctBehavior.insertAfter(
                                    "com.sequoiacm.infrastructure.slowlog.SlowLogManager.getCurrentContext().addExtra("
                                            + name + "," + data + ");",
                                    true);
                        }
                        ctBehavior.insertBefore(
                                "com.sequoiacm.infrastructure.slowlog.SlowLogManager.getCurrentContext().beginOperation(\""
                                        + operation + "\"," + ignoreAfter + ");");
                        ctBehavior.insertAfter(
                                "com.sequoiacm.infrastructure.slowlog.SlowLogManager.getCurrentContext().endOperation();",
                                true);
                        hasChange = true;
                    }
                }
                if (hasChange) {
                    ctClass.toClass();
                    ctClass.detach();
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("failed to init SlowLogManager: currentClass="
                    + currentClassName + ", currentMethod=" + currentMethodName, e);
        }

    }

    public static SlowLogContext getCurrentContext() {
        SlowLogContext slowLogContext = slowLogContextLocal.get();
        if (slowLogContext == null) {
            return EMPTY_CONTEXT;
        }
        return slowLogContext;
    }

    public static void setCurrentContext(SlowLogContext currentContext) {
        slowLogContextLocal.set(currentContext);
    }


}
