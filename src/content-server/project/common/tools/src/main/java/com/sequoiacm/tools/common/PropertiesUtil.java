
package com.sequoiacm.tools.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    // sysconf.properties
    public final static String SAMPLE_VALUE_SCM_LOG_PATH = "SCM_LOG_PATH_VALUE";
    public final static String SAMPLE_VALUE_SCM_AUDIT_SDB_URL = "SCM_AUDIT_SDB_URL";
    public final static String SAMPLE_VALUE_SCM_AUDIT_SDB_USER = "SCM_AUDIT_SDB_USER";
    public final static String SAMPLE_VALUE_SCM_AUDIT_SDB_PASSWD = "SCM_AUDIT_SDB_PASSWD";
    public final static String SAMPLE_VALUE_SCM_AUDIT_SDB_CS_CL = "SCM_AUDIT_SDB_CS_CL";

    public static Properties loadProperties(File file) throws ScmToolsException {

        FileInputStream is;
        try {
            is = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            logger.error("file not found:" + file.getPath(), e);
            throw new ScmToolsException("file not found:" + file.getPath(),
                    ScmExitCode.FILE_NOT_FIND);

        }
        Properties prop = new Properties();
        try {
            prop.load(is);
        }
        catch (IOException e) {
            logger.error("failed to load file:" + file.getParent(), e);
            throw new ScmToolsException(
                    "failed to load file:" + file.getParent() + ",errormsg:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
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


}
