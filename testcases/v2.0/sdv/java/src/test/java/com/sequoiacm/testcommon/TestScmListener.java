package com.sequoiacm.testcommon;

import java.lang.reflect.Field;

import org.apache.log4j.Logger;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestScmListener implements IInvokedMethodListener, ITestListener {
	private static final Logger logger = Logger.getLogger(TestScmListener.class);
	private static final String beginMethodName = "setUp";
	private static final String afterMethodName = "tearDown";
	// private boolean finishSuccess = true;

	private static final String[] testPubClass = { "java.util.List<com.sequoiacm.testcommon.SiteWrapper>",
			"class com.sequoiacm.testcommon.SiteWrapper", "java.util.List<com.sequoiacm.testcommon.WsWrapper>",
			"class com.sequoiacm.testcommon.WsWrapper", "class com.sequoiacm.client.core.ScmSession" };

	/**
	 * print case start time
	 */
	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult result) {
		if (result.getMethod().getMethodName().equals(beginMethodName)) {
			logger.info("[" + result.getInstanceName() + "] start");
		}
	}

	/**
	 * print case end time
	 */
	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult result) {
		if (result.getMethod().getMethodName().equals(afterMethodName)) {
			logger.info("[" + result.getInstanceName() + "] end");
		}

		if (result.getMethod().getMethodName().equals(beginMethodName)) {
			try {
				getScmInfo(result);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private void getScmInfo(ITestResult result) throws IllegalArgumentException, IllegalAccessException {
		String className = result.getInstanceName();
		Field[] fields = result.getInstance().getClass().getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			String type = field.getGenericType().toString();
			Object info = field.get(result.getInstance());
			for (int j = 0; j < testPubClass.length; j++) {
				if (type.equals(testPubClass[j])) {
					logger.info("[" + className + "] " + field.getName() + " info \n[" + info + "]");
				}
			}
		}
	}

	@Override
	public void onTestFailure(ITestResult arg0) {
		// finishSuccess = false;
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
	public void onTestSkipped(ITestResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestStart(ITestResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestSuccess(ITestResult arg0) {
		// TODO Auto-generated method stub

	}
}
