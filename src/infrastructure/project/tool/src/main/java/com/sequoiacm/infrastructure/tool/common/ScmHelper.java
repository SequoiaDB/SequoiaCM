package com.sequoiacm.infrastructure.tool.common;

import com.sequoiacm.infrastructure.common.ScmManifestParser;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

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

    public static String getJarNameByType(final ScmNodeType type) throws ScmToolsException {
        String jarsPath = getPwd() + File.separator + "jars";
        File jarsDir = new File(jarsPath);
        final String matchCondition = "^" + type.getJarNamePrefix() + "(.*)\\.jar$";
        FilenameFilter jarNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Pattern.matches(matchCondition, name);
            }
        };
        File[] files = jarsDir.listFiles(jarNameFilter);
        if (files == null || files.length <= 0) {
            logger.error("Missing jar, jarPrefix:{}, path:{}", matchCondition, jarsPath);
            throw new ScmToolsException("Missing jar, jarPrefix:" + matchCondition + ",path:" + jarsPath,
                    ScmBaseExitCode.FILE_NOT_FIND);
        }
        if (files.length == 1) {
            return files[0].getName();
        }
        Comparator<File> versionComparator = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName());
            }
        };
        Arrays.sort(files, versionComparator);
        logger.info("Multiple jar in the path, jar:{}, path:{}", Arrays.toString(files), jarsPath);
        return files[0].getName();
    }

    public static void configToolsLog(String logFile) throws ScmToolsException {
        InputStream is = ScmHelper.class.getClassLoader().getResourceAsStream(logFile);
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
                        ScmBaseExitCode.SYSTEM_ERROR);
            }
        }
        catch (SecurityException e) {
            logger.error("Failed to create dir:" + file.toString(), e);
            throw new ScmToolsException("Failed to create dir:" + file.toString()
                    + ",permisson error:" + e.getMessage(), ScmBaseExitCode.PERMISSION_ERROR);
        }
    }
}
