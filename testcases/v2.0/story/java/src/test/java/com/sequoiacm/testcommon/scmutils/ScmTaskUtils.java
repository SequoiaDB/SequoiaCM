package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.TestScmBase;
import org.testng.Assert;

import java.util.List;

public class ScmTaskUtils extends TestScmBase {
    // private static final Logger logger = Logger.getLogger(ScmTaskUtils
    // .class);
    private static final int defaultTimeOut = 5 * 60; // 5min

    /**
     * @descreption wait task finish in default timeOut, sync task
     * @param session
     * @param taskId
     * @return
     * @throws Exception
     */
    public static void waitTaskFinish( ScmSession session, ScmId taskId )
            throws Exception {
        waitTaskFinish( session, taskId, defaultTimeOut );
    }

    /**
     * @descreption wait task finish, specify timeOut, sync task
     * @param session
     * @param taskId
     * @param timeOutSec
     * @return
     */
    public static void waitTaskFinish( ScmSession session, ScmId taskId,
            int timeOutSec ) throws Exception {
        int sleepTime = 200; // millisecond
        int maxRetryTimes = ( timeOutSec * 1000 ) / sleepTime;
        int retryTimes = 0;
        while ( true ) {
            ScmTask task = ScmSystem.Task.getTask( session, taskId );
            if ( CommonDefine.TaskRunningFlag.SCM_TASK_FINISH == task
                    .getRunningFlag() ) {
                break;
            } else if ( CommonDefine.TaskRunningFlag.SCM_TASK_ABORT == task
                    .getRunningFlag() ) {
                throw new Exception(
                        "failed, the task running flag is abort, task info : "
                                + "\n" + task.toString() );
            } else if ( CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == task
                    .getRunningFlag() ) {
                throw new Exception(
                        "failed, the task running flag is cancel, task info :"
                                + " \n" + task.toString() );
            } else if ( retryTimes >= maxRetryTimes ) {
                throw new Exception(
                        "failed to wait task finished, maxRetryTimes="
                                + maxRetryTimes + ", task info : \n"
                                + task.toString() );
            }
            Thread.sleep( sleepTime );
            retryTimes++;
        }
    }

    /**
     * @descreption wait asynchronous task finished, default timeOutSec
     * @param ws
     * @param fileId
     * @param expSiteNum
     * @return
     * @throws Exception
     */
    public static void waitAsyncTaskFinished( ScmWorkspace ws, ScmId fileId,
            int expSiteNum ) throws Exception {
        waitAsyncTaskFinished( ws, fileId, 1, expSiteNum );
    }

    /**
     * @descreption wait asynchronous task finished, specify version
     * @param ws
     * @param fileId
     * @param expSiteNum
     * @param version
     *            unit: seconds
     * @return
     * @throws Exception
     */
    public static void waitAsyncTaskFinished( ScmWorkspace ws, ScmId fileId,
            int version, int expSiteNum ) throws Exception {
        waitAsyncTaskFinished( ws, fileId, version, expSiteNum,
                defaultTimeOut );
    }

    /**
     * @descreption wait task stop
     * @param session
     * @param taskId
     * @return
     * @throws Exception
     */
    public static void waitTaskStop( ScmSession session, ScmId taskId )
            throws Exception {
        waitTaskStop( session, taskId, defaultTimeOut );
    }

    /**
     * @descreption wait task stop
     * @param session
     * @param taskId
     * @param timeOutSec
     * @return
     * @throws Exception
     */
    public static void waitTaskStop( ScmSession session, ScmId taskId,
            int timeOutSec ) throws Exception {
        int sleepTime = 200;
        int maxRetryTimes = ( timeOutSec * 1000 ) / sleepTime;
        int retryTimes = 0;
        while ( true ) {
            ScmTask task = ScmSystem.Task.getTask( session, taskId );
            if ( CommonDefine.TaskRunningFlag.SCM_TASK_INIT != task
                    .getRunningFlag()
                    && CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING != task
                            .getRunningFlag() ) {
                break;
            } else if ( retryTimes >= maxRetryTimes ) {
                throw new Exception(
                        "failed to wait task finished, maxRetryTimes="
                                + maxRetryTimes + ", task info : \n"
                                + task.toString() );
            }
            Thread.sleep( sleepTime );
            retryTimes++;
        }
    }

    /**
     * @descreption get node number of site
     * @param session
     * @param siteName
     * @return
     * @throws ScmException
     */
    public static int getNodeNumOfSite( ScmSession session, String siteName )
            throws ScmException {
        List< ScmServiceInstance > contentServerInstanceList = ScmSystem.ServiceCenter
                .getContentServerInstanceList( session );
        int nodeNumOfSite = 0;
        for ( ScmServiceInstance server : contentServerInstanceList ) {
            if ( server.getServiceName().equals( siteName ) ) {
                nodeNumOfSite++;
            }
        }
        return nodeNumOfSite;
    }

    /**
     * @descreption wait asynchronous task finished, specify timeOutSec
     * @param ws
     * @param fileId
     * @param majorVersion
     * @param expSiteNum
     * @param timeOutSec
     *            unit: seconds
     * @return
     * @throws Exception
     */
    public static void waitAsyncTaskFinished( ScmWorkspace ws, ScmId fileId,
            int majorVersion, int expSiteNum, int timeOutSec )
            throws Exception {
        int sleepTime = 200; // millisecond
        int maxRetryTimes = timeOutSec * 1000 / sleepTime;
        int retryTimes = 0;
        while ( true ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId,
                    majorVersion, 0 );
            int size = file.getLocationList().size();
            if ( size == expSiteNum ) {
                break;
            } else if ( retryTimes >= maxRetryTimes ) {
                throw new Exception( "wait async task, retry failed." );
            }
            Thread.sleep( sleepTime );
            retryTimes++;
        }
    }

    /**
     * @descreption wait asynchronous task finished, default timeOutSec not sure
     *              about migrating file versions
     * @param ws
     * @param fileId
     * @param expSiteNum
     * @return asyncTask success file version
     * @throws Exception
     */
    public static int waitAsyncTaskFinished2( ScmWorkspace ws, ScmId fileId,
            int version, int expSiteNum ) throws Exception {
        return waitAsyncTaskFinished2( ws, fileId, version, expSiteNum,
                defaultTimeOut );
    }

    /**
     * @descreption wait asynchronous task finished, specify timeOutSec migrating file versions
     * @param ws
     * @param fileId
     * @param majorVersion
     * @param expSiteNum
     * @param timeOutSec
     *            unit: seconds
     * @return asyncTask success file version
     * @throws Exception
     */
    public static int waitAsyncTaskFinished2( ScmWorkspace ws, ScmId fileId,
            int majorVersion, int expSiteNum, int timeOutSec )
            throws Exception {
        int sleepTime = 200; // millisecond
        int maxRetryTimes = timeOutSec * 1000 / sleepTime;
        int retryTimes = 0;
        int asyncVersion = 1;
        while ( true ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId,
                    majorVersion, 0 );
            int size = file.getLocationList().size();
            if ( size == expSiteNum ) {
                asyncVersion = majorVersion;
                break;
            } else if ( retryTimes >= maxRetryTimes ) {
                throw new Exception( "wait async task, retry failed." );
            }
            try {
                file = ScmFactory.File.getInstance( ws, fileId,
                        majorVersion + 1, 0 );
                size = file.getLocationList().size();
                if ( size == expSiteNum ) {
                    asyncVersion = majorVersion + 1;
                    break;
                }
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        e.getMessage() );
            }
            Thread.sleep( sleepTime );
            retryTimes++;
        }
        return asyncVersion;
    }
}
