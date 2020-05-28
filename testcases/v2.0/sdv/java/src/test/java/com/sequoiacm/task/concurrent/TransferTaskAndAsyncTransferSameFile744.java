package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-744:并发迁移任务、单个异步迁移相同文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class TransferTaskAndAsyncTransferSameFile744 extends TestScmBase {
    private static final int defaultTimeOut = 5 * 60; // 5min
    private boolean runSuccess = false;

    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private String author = "TD744";
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileSize = 10;
    private int fileNum = 200;
    private File localPath = null;
    private String filePath = null;
    private ScmId taskId = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    // private NodeWrapper node = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready local file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            // node = branceSite.getNode();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder.start()
                    .put( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // login
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            // ready file
            this.writeFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            StartTransferTask transferTask = new StartTransferTask();
            transferTask.start();

            AsyncTransfer asyncTransfer = new AsyncTransfer();
            asyncTransfer.start();

            if ( !( transferTask.isSuccess() && asyncTransfer.isSuccess() ) ) {
                Assert.fail( transferTask.getErrorMsg()
                        + asyncTransfer.getErrorMsg() );
            }

            // check results
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.getInstance( wsA, fileId ).delete( true );
                }
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void writeFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile scmfile = ScmFactory.File.createInstance( wsA );
            scmfile.setContent( filePath );
            scmfile.setFileName( author + "_" + UUID.randomUUID() );
            scmfile.setAuthor( author );
            fileIdList.add( scmfile.save() );
        }
    }

    private void waitTaskFinish( ScmSession session, ScmId taskId )
            throws Exception {
        int sleepTime = 200; // millisecond
        int maxRetryTimes = ( defaultTimeOut * 1000 ) / sleepTime;
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

    private class StartTransferTask extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            ScmWorkspace wsA = null;
            String wsName = ws_T.getName();
            try {
                sessionA = TestScmTools.createSession( branceSite );
                wsA = ScmFactory.Workspace.getWorkspace( wsName, sessionA );

                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                taskId = ScmSystem.Task.startTransferTask( wsA, condition );
                waitTaskFinish( sessionA, taskId );

                // check task info
                ScmTask taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
                // check results
                Assert.assertEquals( taskInfo.getWorkspaceName(), wsName );
                Assert.assertEquals( taskInfo.getType(),
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

    private class AsyncTransfer extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession sessionA = null;
            ScmWorkspace wsA = null;
            String wsName = ws_T.getName();
            try {
                sessionA = TestScmTools.createSession( branceSite );
                wsA = ScmFactory.Workspace.getWorkspace( wsName, sessionA );

                for ( int i = 0; i < fileNum; i++ ) {
                    int j = new Random().nextInt( fileNum );
                    ScmFactory.File.asyncTransfer( wsA, fileIdList.get( j ) );
                    SiteWrapper[] expSiteList = { rootSite, branceSite };
                    ScmTaskUtils.waitAsyncTaskFinished( wsA,
                            fileIdList.get( j ), expSiteList.length );
                    ScmFileUtils.checkMetaAndData( ws_T, fileIdList.get( j ),
                            expSiteList, localPath, filePath );
                }
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }
}
