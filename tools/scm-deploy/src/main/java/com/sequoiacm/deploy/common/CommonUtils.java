package com.sequoiacm.deploy.common;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.bson.util.JSONParseException;
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

    public static BSONObject parseJsonFile(String filePath) throws Exception {
        String json = readContentFromLocalFile(filePath);
        try {
            return (BSONObject) JSON.parse(json);
        }
        catch (JSONParseException e) {
            throw new IllegalArgumentException("json syntax error, file=" + filePath, e);
        }
    }

    public static String readContentFromLocalFile(String filepath) throws Exception {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filepath);
            return IOUtils.toString(is, "utf-8");
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file not found:" + filepath, e);
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
