package com.sequoiacm.deploy.common;

import com.sequoiacm.deploy.config.CommonConfig;
import org.apache.commons.io.IOUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.bson.util.JSONParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CommonUtils {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);
    public static final List<String> crontabCommands = new ArrayList<>();

    static {
        crontabCommands.add("service cron status");
        crontabCommands.add("service crond status");
    }

    public static void checkNoNull(Object obj, String msg) {
        if (obj == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void closeResource(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                logger.warn("failed to close resource:{}", c, e);
            }
        }
    }

    public static BSONObject parseJsonFile(String filePath) throws Exception {
        String json = readContentFromLocalFile(filePath);
        try {
            return (BSONObject) JSON.parse(json);
        } catch (JSONParseException e) {
            throw new IllegalArgumentException("json syntax error, file=" + filePath, e);
        }
    }

    public static String readContentFromLocalFile(String filepath) throws Exception {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filepath);
            return IOUtils.toString(is, "utf-8");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file not found:" + filepath, e);
        } finally {
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

    public static File packDirs(String basePath, String tarName, List<String> fileList) {
        StringBuilder cmd = new StringBuilder("cd " + basePath + ";tar -cf " + tarName);
        for (String file : fileList) {
            cmd.append(" ").append(file);
        }
        String[] finalCmd = new String[] { "/bin/bash", "-c", cmd.toString() };
        Process ps = null;
        try {
            ps = Runtime.getRuntime().exec(finalCmd);
            ps.waitFor();
        }
        catch (Exception e) {
            throw new RuntimeException("fail to pack dirs", e);
        }finally {
            if (ps != null) {
                ps.destroy();
            }
        }
        return new File(basePath + "/" + tarName);
    }

    public static boolean confirmExecute(String operate) {
        while (true) {
            logger.info("Whether to " + operate + "? Please enter (y/N) confirm");
            Scanner s = new Scanner(System.in);
            String answer = s.nextLine().toLowerCase();
            if ("y".equals(answer) || "yes".equals(answer)) {
                return true;
            }
            else if ("".equals(answer) || "n".equals(answer) || "no".equals(answer)) {
                return false;
            }
        }
    }

    public static int getWaitServiceReadyTimeout() {
        int timeoutMs = CommonConfig.getInstance().getWaitServiceReadyTimeout();
        if (timeoutMs > 5000) {
            return timeoutMs / 1000;
        }

        return 120;
    }

    public static String removeRepeatFileSparator(String str) {
        StringBuilder sb = new StringBuilder();
        if (str == null) {
            return null;
        }
        char previousChar = 0;
        for (char c : str.toCharArray()) {
            if (!File.separator.equals(String.valueOf(c)) || !File.separator.equals(String.valueOf(previousChar))) {
                sb.append(c);
            }
            previousChar = c;
        }
        return sb.toString();
    }
}
