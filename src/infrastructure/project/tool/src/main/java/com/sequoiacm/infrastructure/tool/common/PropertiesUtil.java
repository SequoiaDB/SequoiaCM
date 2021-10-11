
package com.sequoiacm.infrastructure.tool.common;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    // sysconf.properties
    public final static String SAMPLE_VALUE_SCM_LOG_PATH = "SCM_LOG_PATH_VALUE";

    public static Properties loadProperties(File file) throws ScmToolsException {

        FileInputStream is;
        try {
            is = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            logger.error("file not found:" + file.getPath(), e);
            throw new ScmToolsException("file not found:" + file.getPath(),
                    ScmBaseExitCode.FILE_NOT_FIND);

        }
        Properties prop = new Properties();
        try {
            prop.load(is);
        }
        catch (IOException e) {
            logger.error("failed to load file:" + file.getParent(), e);
            throw new ScmToolsException(
                    "failed to load file:" + file.getParent() + ",errormsg:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            try {
                is.close();
            }
            catch (Exception e) {
                logger.warn("close inputStream error,file:" + file.getPath(), e);
            }
        }

        return prop;
    }

    public static Properties loadProperties(String filePath) throws ScmToolsException {
        File toolLog4j = new File(filePath);
        return loadProperties(toolLog4j);
    }

    public static void writeProperties(Map<String, String> items, String propPath)
            throws ScmToolsException {
        File file = new File(propPath);
        if (!file.exists()) {
            ScmCommon.createFile(propPath);
        }
        Properties prop = new Properties();
        for (String key : items.keySet()) {
            prop.setProperty(key, items.get(key));
        }
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(file);
            prop.store(pw, "UTF-8");
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to write " + file.getName(),
                    ScmBaseExitCode.FILE_NOT_FIND, e);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to write " + file.getName(), ScmBaseExitCode.SYSTEM_ERROR,
                    e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to write " + file.getName(),
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (pw != null) {
                try {
                    pw.close();
                }
                catch (Exception e) {
                    logger.warn("Failed to close resource:{}", pw, e);
                }
            }
        }
    }
}
