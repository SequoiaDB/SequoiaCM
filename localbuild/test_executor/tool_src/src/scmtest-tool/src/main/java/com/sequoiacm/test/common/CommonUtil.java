package com.sequoiacm.test.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.bson.util.JSONParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static void closeResource(Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                }
                catch (Exception e) {
                    logger.warn("Failed to close resource:{}", c, e);
                }
            }
        }
    }

    public static BSONObject parseJsonFile(String filePath) throws IOException {
        String json = readContentFromLocalFile(filePath);
        try {
            return (BSONObject) JSON.parse(json);
        }
        catch (JSONParseException e) {
            throw new IOException("Json syntax error, file=" + filePath, e);
        }
    }

    public static BSONObject parseJsonString(String jsonString) {
        try {
            return (BSONObject) JSON.parse(jsonString);
        }
        catch (JSONParseException e) {
            throw new IllegalArgumentException("Json syntax error:", e);
        }
    }

    public static String readContentFromLocalFile(String filepath) throws IOException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filepath);
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        catch (FileNotFoundException e) {
            throw new IOException("File not found:" + filepath, e);
        }
        finally {
            CommonUtil.closeResource(is);
        }
    }

    public static void cleanOrInitDir(String... dirPaths) throws IOException {
        for (String dirPath : dirPaths) {
            File dir = new File(dirPath);
            if (!dir.exists()) {
                createDir(dirPath);
                continue;
            }
            try {
                FileUtils.cleanDirectory(dir);
            }
            catch (IOException e) {
                throw new IOException("Failed to clean directory, cause by: " + e);
            }
        }
    }

    public static void createDir(String dirPath) throws IOException {
        File dir = new File(dirPath);
        try {
            if (!dir.exists()) {
                FileUtils.forceMkdir(dir);
            }
        }
        catch (IOException e) {
            throw new IOException("Failed to create directory, cause by: " + e);
        }
    }

    public static void assertTrue(boolean f, String message) {
        if (!f) {
            throw new IllegalArgumentException(message);
        }
    }
}
