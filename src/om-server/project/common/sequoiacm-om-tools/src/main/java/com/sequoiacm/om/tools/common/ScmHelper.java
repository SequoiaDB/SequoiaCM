package com.sequoiacm.om.tools.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.ScmManifestParser;
import com.sequoiacm.om.tools.SchCtl;
import com.sequoiacm.om.tools.element.ScmNodeType;
import com.sequoiacm.om.tools.exception.ScmExitCode;
import com.sequoiacm.om.tools.exception.ScmToolsException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;

public class ScmHelper {
    private static String pwd;
    private static Logger logger = LoggerFactory.getLogger(ScmHelper.class);

    public static String getPwd() {
        if (pwd == null) {
            File pwdFile = new File("");
            pwd = pwdFile.getAbsolutePath();
            return pwd;
        }
        return pwd;
    }

    public static String getJarNameByType(ScmNodeType type) throws ScmToolsException {
        String version;
        try {
            version = ScmManifestParser.getManifestInfoFromJar(ScmHelper.class).getScmVersion();
        }
        catch (IOException e) {
            throw new ScmToolsException("failed to load manifest info:", ScmExitCode.SYSTEM_ERROR,
                    e);
        }
        if (version == null) {
            version = "1.0-SNAPSHOT";
            logger.warn("load version from manifest error,use default version:{}", version);
        }
        return type.getJarNamePrefix() + version + ".jar";
    }

    public static Properties loadManifest(Class<?> classInTheJar) throws ScmToolsException {
        Properties prop = new Properties();

        String className = classInTheJar.getSimpleName() + ".class";
        String classPath = classInTheJar.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // when ide start the app.
            logger.warn("class not from jar,loadManifest do noting:class={}", classPath);
            return prop;
        }

        String manifestPath = classPath.substring(0, classPath.indexOf("!") + 1)
                + "/META-INF/MANIFEST.MF";
        URL manifestUrl = null;
        try {
            manifestUrl = new URL(manifestPath);
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to open manifest file:url=" + manifestPath,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        logger.info("load version info from manifest:url={}", manifestUrl.toString());

        InputStream is = null;
        try {
            is = manifestUrl.openStream();
            Manifest manifest = new Manifest(is);
            Attributes a = manifest.getMainAttributes();

            for (Entry<Object, Object> set : a.entrySet()) {
                prop.put(set.getKey().toString(), set.getValue().toString());
            }
            return prop;
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to load manifest file:url=" + manifestUrl,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (Exception e2) {
                logger.warn("failed to close inputstream:url={}", manifestUrl, e2);
            }
        }
    }

    public static void configToolsLog(String logFile) throws ScmToolsException {
        InputStream is = SchCtl.class.getClassLoader().getResourceAsStream(logFile);
        try {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configure = new JoranConfigurator();
            configure.setContext(lc);
            FixedWindowRollingPolicy f;
            lc.reset();
            configure.doConfigure(is);
        }
        catch (JoranException e) {
            e.printStackTrace();
            logger.error("config logback failed", e);
        }
        finally {
            try {
                is.close();
            }
            catch (Exception e) {
                logger.warn("close inpustream occur error", e);
            }
        }
    }

    public static void createDir(String dirPath) throws ScmToolsException {
        File file = new File(dirPath);
        try {
            if (!file.exists() && file.mkdirs() != true) {
                logger.error("Faile to create dir:" + file.toString());
                throw new ScmToolsException("Faile to create dir:" + file.toString(),
                        ScmExitCode.COMMON_UNKNOW_ERROR);
            }
        }
        catch (SecurityException e) {
            logger.error("Failed to create dir:" + file.toString(), e);
            throw new ScmToolsException("Failed to create dir:" + file.toString()
                    + ",permisson error:" + e.getMessage(), ScmExitCode.PERMISSION_ERROR);
        }
    }
}
