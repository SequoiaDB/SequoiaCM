package com.sequoiacm.testcommon;

import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestScmListener implements ITestListener {
    private static final Logger logger = Logger.getLogger(TestScmListener.class);

    @Override
    public void onTestFailure(ITestResult r) {
        logger.error("test is failed:name=" + getClassMethodName(r), r.getThrowable());
    }

    /**
     * print session/file's name when successful execution of all test cases
     */
    @Override
    public void onFinish(ITestContext arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStart(ITestContext arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTestSkipped(ITestResult r) {
        logger.warn("test is skipped:name=" + getClassMethodName(r));

    }

    @Override
    public void onTestStart(ITestResult r) {
        logger.info("test is started:name=" + getClassMethodName(r));
    }

    @Override
    public void onTestSuccess(ITestResult r) {
        logger.info("test is success:name=" + getClassMethodName(r));
    }

    private String getClassMethodName(ITestResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getInstanceName());

        if (null != r.getMethod()) {
            sb.append(":").append(r.getMethod().getMethodName());
        }

        return sb.toString();
    }
}
