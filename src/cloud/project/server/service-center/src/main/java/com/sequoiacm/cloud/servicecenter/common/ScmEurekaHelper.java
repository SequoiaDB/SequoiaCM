package com.sequoiacm.cloud.servicecenter.common;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmEurekaHelper {
    private static final Logger logger = LoggerFactory.getLogger(ScmEurekaHelper.class);

    private static boolean evictionTaskLogHasDisabled = false;

    /**
     * 关闭 Eureka EvictionTask 中的 logger.info 日志打印，避免产生频繁的日志输出
     * 
     * @see com.netflix.eureka.registry.AbstractInstanceRegistry.EvictionTask#run()
     */
    public synchronized static void disableEvictionTaskLog() {
        if (evictionTaskLogHasDisabled) {
            return;
        }
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        try {
            CtClass ctClass = pool
                    .get("com.netflix.eureka.registry.AbstractInstanceRegistry$EvictionTask");
            CtMethod run = ctClass.getDeclaredMethod("run");
            ScmExprEditor scmExprEditor = new ScmExprEditor();
            run.instrument(scmExprEditor);
            ctClass.toClass();
            ctClass.detach();
            if (scmExprEditor.isChange()) {
                evictionTaskLogHasDisabled = true;
                logger.info("eviction task log disabled");
            }
            else {
                logger.warn("failed to disable eviction task log");
            }
        }
        catch (Exception e) {
            logger.warn("failed to disable eviction task log", e);
        }
    }

    private static class ScmExprEditor extends ExprEditor {
        private boolean change = false;

        public boolean isChange() {
            return change;
        }

        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            // 移除 logger.info
            if ("info".equals(m.getMethodName())) {
                m.replace("");
                change = true;
            }
            super.edit(m);
        }
    }
}
