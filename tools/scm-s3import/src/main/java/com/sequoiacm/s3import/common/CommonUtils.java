package com.sequoiacm.s3import.common;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.module.S3Bucket;

import java.io.*;
import java.util.List;
import java.util.Locale;

public class CommonUtils {

    public static String readInputStream(InputStream is) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            return content.toString();
        }
        finally {
            ScmCommon.closeResource(isr, br);
        }
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            }
            else if (hex.length() == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }

    public static void checkMaxExecTime(long startTime, long runStartTime, long maxExecTime)
            throws ScmToolsException {
        if (maxExecTime == -1) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long residualTime = maxExecTime - (currentTime - startTime);
        if (residualTime < 0) {
            throw new ScmToolsException(
                    "Process have been interrupted because of timeout: startTime=" + startTime
                            + " ,now=" + currentTime + " ,maxExecTime=" + maxExecTime + "(ms)",
                    S3ImportExitCode.EXEC_TIME_OUT);
        }

        long lastRunTime = currentTime - runStartTime;
        if (residualTime * 10 < lastRunTime) {
            throw new ScmToolsException(
                    "Process have been interrupted because of not enough time to execute: startTime="
                            + startTime + " ,now=" + currentTime + " ,maxExecTime=" + maxExecTime
                            + " ,residualTime=" + residualTime + " ,lastRunTime=" + lastRunTime
                            + "(ms)",
                    S3ImportExitCode.EXEC_TIME_OUT);
        }
    }

    public static void checkFailCount(long currentFailCount, long maxFailCount)
            throws ScmToolsException {
        if (currentFailCount > maxFailCount) {
            throw new ScmToolsException(
                    "Process have been interrupted because of too many failed tasks: failCount="
                            + currentFailCount + ",maxFailCount=" + maxFailCount,
                    S3ImportExitCode.TOO_MANY_FAILURE);
        }
    }

    public static String getStandardFilePath(String filePath) {
        return filePath.endsWith(File.separator) ? filePath : filePath + File.separator;
    }

    public static <T> T parseJsonStr(String jsonStr, Class<T> clazz) {
        Gson gson = new Gson().newBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        return gson.fromJson(jsonStr, clazz);
    }

    public static String toJSONString(Object o) {
        // 驼峰转换
        Gson gson = new Gson().newBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        return gson.toJson(o);
    }

    public static String toPrettyJson(JsonElement jsonElement) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return gson.toJson(jsonElement);
    }

    public static String bucketListToStr(List<S3Bucket> bucketList) {
        StringBuilder str = new StringBuilder().append("[");
        for (int i = 0; i < bucketList.size(); i++) {
            if (i != 0) {
                str.append(", ");
            }
            str.append(bucketList.get(i).getName());
        }
        return str.append("]").toString();
    }

    public static String getPaddingKey(String key) {
        StringBuilder sb = new StringBuilder(key);
        for (int i = key.length(); i < 16; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public static void assertTrue(boolean f, String message) throws ScmToolsException {
        if (!f) {
            throw new ScmToolsException(message, S3ImportExitCode.INVALID_ARG);
        }
    }
}
