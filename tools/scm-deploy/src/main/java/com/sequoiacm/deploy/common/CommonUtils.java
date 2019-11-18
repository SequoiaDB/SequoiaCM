package com.sequoiacm.deploy.common;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtils {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);

    public static void checkNoNull(Object obj, String msg) {
        if (obj == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void closeResource(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (Exception e) {
                logger.warn("failed to close resource:{}", c, e);
            }
        }
    }

    public static String readContentFromLocalFile(String filepath) throws Exception {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filepath);
            return IOUtils.toString(is, "utf-8");
        }
        catch (Exception e) {
            throw new Exception("failed to read file:" + filepath, e);
        }
        finally {
            CommonUtils.closeResource(is);
        }
    }

    public static String toString(List<?> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (Object s : list) {
            sb.append(s.toString()).append(sep);
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - sep.length());
        }
        return sb.toString();

    }

    public static String getLogFilePath() {
        return new File("./deploy.log").getPath();
    }

    public static void assertTrue(boolean f, String message) {
        if (!f) {
            throw new IllegalArgumentException(message);
        }
    }
}
