package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.TestScmBase;

public class ScmTaskUtils extends TestScmBase {
//	private static final Logger logger = Logger.getLogger(ScmTaskUtils.class);
	private static final int defaultTimeOut = 5 * 60; // 5min

	/**
	 * wait task finish in default timeOut, sync task
	 * 
	 * @param session
	 * @param taskId
	 * @throws Exception
	 */
	public static void waitTaskFinish(ScmSession session, ScmId taskId) throws Exception {
		waitTaskFinish(session, taskId, defaultTimeOut);
	}

	/**
	 * wait task finish, specify timeOut, sync task
	 * 
	 * @param timeOutSec
	 *            ........
	 */
	public static void waitTaskFinish(ScmSession session, ScmId taskId, int timeOutSec) throws Exception {
		int sleepTime = 200; // millisecond
		int maxRetryTimes = (timeOutSec * 1000) / sleepTime;
		int retryTimes = 0;
		while (true) {
			ScmTask task = ScmSystem.Task.getTask(session, taskId);
			if (CommonDefine.TaskRunningFlag.SCM_TASK_FINISH == task.getRunningFlag()) {
				break;
			} 
			else if (CommonDefine.TaskRunningFlag.SCM_TASK_ABORT == task.getRunningFlag()) {
				throw new Exception("failed, the task running flag is abort, task info : \n" + task.toString());
			} 
			else if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == task.getRunningFlag()) {
				throw new Exception("failed, the task running flag is cancel, task info : \n" + task.toString());
			} 
			else if (retryTimes >= maxRetryTimes) {
				throw new Exception("failed to wait task finished, maxRetryTimes=" + maxRetryTimes
						+ ", task info : \n" + task.toString());
			}
			Thread.sleep(sleepTime);
			retryTimes++;
		}
	}

	/**
	 * wait asynchronous task finished, default timeOutSec
	 * 
	 * @param ws
	 * @param fileId
	 * @param expSiteNum
	 * @throws Exception
	 */
	public static void waitAsyncTaskFinished(ScmWorkspace ws, ScmId fileId, int expSiteNum) throws Exception {
		waitAsyncTaskFinished(ws, fileId, expSiteNum, defaultTimeOut);
	}

	/**
	 * wait asynchronous task finished, specify timeOutSec
	 * 
	 * @param ws
	 * @param fileId
	 * @param expSiteNum
	 * @param timeOutSec
	 *            unit: seconds
	 * @throws Exception
	 */
	public static void waitAsyncTaskFinished(ScmWorkspace ws, ScmId fileId, int expSiteNum, int timeOutSec)
			throws Exception {
		int sleepTime = 200; // millisecond
		int maxRetryTimes = (timeOutSec * 1000) / sleepTime;
		int retryTimes = 0;
		while (true) {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			int size = file.getLocationList().size();
			if (size == expSiteNum) {
				break;
			} else if (retryTimes >= maxRetryTimes) {
				throw new Exception("wait async task, retry failed.");
			}
			Thread.sleep(sleepTime);
			retryTimes++;
		}
	}

}
