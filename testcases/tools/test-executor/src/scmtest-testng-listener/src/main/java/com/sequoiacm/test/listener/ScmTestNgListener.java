package com.sequoiacm.test.listener;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScmTestNgListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestNgListener.class);
    private static String testProgressFilePath = System.getProperty("basePath") + "tmp"
            + File.separator + "test-progress.json";

    private int startAndSkipTestCount;
    private int total;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private Set<String> runningTestcase = ConcurrentHashMap.newKeySet();
    private Timer timer = new Timer(true);

    @Override
    public void onStart(ITestContext iTestContext) {
        total = iTestContext.getAllTestMethods().length;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                refreshResultFile();
            }
        };
        timer.schedule(timerTask, 0, 2000);
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        timer.cancel();
        refreshResultFile();
    }

    @Override
    public synchronized void onTestStart(ITestResult iTestResult) {
        runningTestcase.add(iTestResult.getTestClass().getName());
        if (++startAndSkipTestCount > total) {
            total = startAndSkipTestCount;
        }
    }

    @Override
    public synchronized void onTestSuccess(ITestResult iTestResult) {
        runningTestcase.remove(iTestResult.getTestClass().getName());
        successCount++;
    }

    @Override
    public synchronized void onTestFailure(ITestResult iTestResult) {
        runningTestcase.remove(iTestResult.getTestClass().getName());
        failedCount++;
    }

    @Override
    public synchronized void onTestSkipped(ITestResult iTestResult) {
        skippedCount++;
        if (++startAndSkipTestCount > total) {
            total = startAndSkipTestCount;
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    private synchronized void refreshResultFile() {
        JSONObject testProgress = new JSONObject();
        testProgress.put("total", total);
        testProgress.put("success", successCount);
        testProgress.put("failed", failedCount);
        testProgress.put("skipped", skippedCount);
        testProgress.put("running", setToString(runningTestcase, ", "));

        BufferedWriter writer = null;
        OutputStreamWriter opsWriter = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(testProgressFilePath, false);
            opsWriter = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer = new BufferedWriter(opsWriter);
            writer.write(testProgress.toString());
        }
        catch (IOException e) {
            logger.error("Write data error, cause by:" + e);
        }
        finally {
            closeResources(writer, opsWriter, fos);
        }
    }

    public String setToString(Set<?> set, String sep) {
        StringBuilder sb = new StringBuilder();
        for (Object s : set) {
            sb.append(s.toString()).append(sep);
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - sep.length());
        }
        return sb.toString();
    }

    private void closeResources(Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                }
                catch (IOException e) {
                    logger.error("Failed to close resource, cause by:" + e);
                }
            }
        }
    }
}
