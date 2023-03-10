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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ScmTestNgListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestNgListener.class);
    private static String tmpDirPath = System.getProperty("basePath") + "tmp" + File.separator;
    private static String testProgressFilePath = tmpDirPath + "test-progress.json";
    private static String errorDetailFilePath = tmpDirPath + "error.detail";

    private long total;
    private AtomicLong startAndSkipTestCount = new AtomicLong(0);
    private AtomicLong successCount = new AtomicLong(0);
    private AtomicLong failedCount = new AtomicLong(0);
    private AtomicLong skippedCount = new AtomicLong(0);
    private Set<String> runningTestcase = ConcurrentHashMap.newKeySet();
    private Queue<ErrorTestCase> errorTestCaseQueue = new ConcurrentLinkedQueue<>();
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
    public void onTestStart(ITestResult iTestResult) {
        // 如果执行过程，打印正在执行的全量用例名（e.g.
        // com.sequoiacm.s3.partupload.concurrent.MultipartUpload4336），跑并发用例时可读性较差
        // 用例名中存在编号作为唯一标志，因此只需要记录类名（e.g. MultipartUpload4336）即可
        String fullnameOfTestcase = iTestResult.getTestClass().getName();
        runningTestcase.add(fullnameOfTestcase.substring(fullnameOfTestcase.lastIndexOf(".") + 1));
        startAndSkipTestCount.incrementAndGet();
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        String fullnameOfTestcase = iTestResult.getTestClass().getName();
        runningTestcase
                .remove(fullnameOfTestcase.substring(fullnameOfTestcase.lastIndexOf(".") + 1));
        successCount.incrementAndGet();
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        iTestResult.getThrowable().printStackTrace(pw);
        String fullnameOfTestcase = iTestResult.getTestClass().getName();
        errorTestCaseQueue.add(new ErrorTestCase(fullnameOfTestcase, sw.toString()));
        runningTestcase
                .remove(fullnameOfTestcase.substring(fullnameOfTestcase.lastIndexOf(".") + 1));
        failedCount.incrementAndGet();
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        skippedCount.incrementAndGet();
        startAndSkipTestCount.incrementAndGet();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    private synchronized void refreshResultFile() {
        // 1. 更新错误用例信息
        if (errorTestCaseQueue.size() > 0) {
            StringBuilder errorDetails = new StringBuilder();
            ErrorTestCase curError;
            while ((curError = errorTestCaseQueue.poll()) != null) {
                errorDetails.append(curError);
            }
            updateResourceFile(errorDetailFilePath, errorDetails.toString(), true);
        }
        // 2. 更新执行进度
        JSONObject testProgress = new JSONObject();
        testProgress.put("total", Math.max(total, startAndSkipTestCount.get()));
        testProgress.put("success", successCount);
        testProgress.put("failed", failedCount);
        testProgress.put("skipped", skippedCount);
        testProgress.put("running", setToString(runningTestcase, ", "));
        updateResourceFile(testProgressFilePath, testProgress.toString(), false);
    }

    private void updateResourceFile(String filePath, String content, boolean isAppend) {
        BufferedWriter writer = null;
        OutputStreamWriter opsWriter = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath, isAppend);
            opsWriter = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer = new BufferedWriter(opsWriter);
            writer.write(content);
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

class ErrorTestCase {

    private String testcaseName;
    private String errorDetail;

    public ErrorTestCase(String testcaseName, String errorDetail) {
        this.testcaseName = testcaseName;
        this.errorDetail = errorDetail;
    }

    @Override
    public String toString() {
        return "[error_test]:" + testcaseName + "\n" + "[error_stack]:" + errorDetail + "\n";
    }
}