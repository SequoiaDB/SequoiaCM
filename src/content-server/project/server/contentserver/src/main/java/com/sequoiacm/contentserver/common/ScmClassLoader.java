package com.sequoiacm.contentserver.common;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(ScmClassLoader.class);

    private static final String addURLMethod = "addURL";

    private URLClassLoader innerLoader = null;
    private Method method = null;
    private boolean isAccessible = false;
    private Set<String> fileSetter = new HashSet<>();

    private static ScmClassLoader instance = null;

    public static ScmClassLoader getInstance() {
        if (null == instance) {
            synchronized (ScmClassLoader.class) {
                if (null == instance) {
                    try {
                        instance = new ScmClassLoader();
                    }
                    catch (ScmServerException e) {
                        logger.error("failed to create ScmClassLoader", e);
                        System.exit(-1);
                    }
                }
            }
        }

        return instance;
    }

    private ScmClassLoader() throws ScmServerException {
        innerLoader = (URLClassLoader)ScmClassLoader.class.getClassLoader();
        logger.info("innerLoader={}", innerLoader);
        if (null == innerLoader) {
            throw new ScmSystemException(
                    "failed to get system class loader");
        }

        try {
            method = URLClassLoader.class.getDeclaredMethod(addURLMethod, URL.class);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "get method failed:class=URLClassLoader,method=" + addURLMethod, e);
        }

        isAccessible = method.isAccessible();
    }

    public void addJar(String jarDir) throws ScmServerException {
        File jarDirFile = new File(jarDir);
        if (fileSetter.contains(jarDirFile.getAbsolutePath())) {
            //have already loaded
            return;
        }

        if (jarDirFile.isFile()) {
            addJar(new File[]{ jarDirFile });
        }
        else {
            File[] files = jarDirFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar") || name.endsWith(".zip");
                }
            });

            if (null != files) {
                addJar(files);
            }
        }

        fileSetter.add(jarDirFile.getAbsolutePath());
    }

    private void addJar(File[] jars) throws ScmServerException {
        if (!isAccessible) {
            //make sure function can be called
            method.setAccessible(true);
        }

        try {
            for (File oneJar : jars) {
                try {
                    URL url = oneJar.toURI().toURL();
                    logger.debug("load url:url={}", url);
                    method.invoke(innerLoader, url);
                }
                catch (Exception e) {
                    throw new ScmSystemException(
                            "invoke method failed:file=" + oneJar.getAbsolutePath()
                            + ",method=" + method, e);
                }
            }
        }
        finally {
            //restore the access flag
            method.setAccessible(isAccessible);
        }
    }

    public Class<?> loadClass(String className) throws ScmServerException {
        try {
            return innerLoader.loadClass(className);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "load class failed:className=" + className, e);
        }
    }
}
