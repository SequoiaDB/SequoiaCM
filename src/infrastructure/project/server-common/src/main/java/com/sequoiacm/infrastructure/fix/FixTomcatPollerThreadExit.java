package com.sequoiacm.infrastructure.fix;


import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.apache.catalina.util.ServerInfo;

public class FixTomcatPollerThreadExit {

    private static FixTomcatPollerThreadExit INSTANCE = null;
    private static final String CLASS_NAME = "org.apache.tomcat.util.net.NioEndpoint$Poller";
    private static final String METHOD_NAME = "processKey";
    private boolean isFixed = false;
    private static final String TOMCAT_VERSION = "8.5.31.0";

    private FixTomcatPollerThreadExit() {
    }

    /**
     * 修改Tomcat中当poller无法创建新线程,导致poller线程直接退出而未断开现有连接的错误
     */
    public synchronized void fix() {
        String tomcatVersion = ServerInfo.getServerNumber();
        if (!TOMCAT_VERSION.equals(tomcatVersion)){
            throw new RuntimeException("The changes made to tomcat are only valid for version" + TOMCAT_VERSION);
        }
        if (isFixed) {
            return;
        }
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        CtClass ctClass = null;
        CtMethod ctMethod = null;
        try {
            ctClass = pool.getCtClass(CLASS_NAME);
            ctMethod = ctClass.getDeclaredMethod(METHOD_NAME);
        } catch (NotFoundException e) {
            return;
        }
        try {
            ctMethod.insertAt(876, " try {\n" +
                    "                    org.apache.tomcat.util.ExceptionUtils.handleThrowable(t);\n" +
                    "                }catch (Throwable e){\n" +
                    "                    cancelledKey(sk);\n" +
                    "                    org.slf4j.LoggerFactory.getLogger(org.apache.tomcat.util.net.NioEndpoint$Poller.class).error(\"\", e);\n" +
                    "                    return;\n" +
                    "                }");
            ctClass.toClass();
            ctClass.detach();
            isFixed = true;
        } catch (Exception e) {
            throw new RuntimeException("failed to fix: TomcatInstanceIncorrectStatus", e);
        }

    }

    public static FixTomcatPollerThreadExit getInstance() {
        if (INSTANCE == null) {
            synchronized (FixTomcatPollerThreadExit.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FixTomcatPollerThreadExit();
                }
            }
        }
        return INSTANCE;
    }
}
