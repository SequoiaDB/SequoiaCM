package com.sequoiacm.task.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
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

/**
 * @FileName SCM-439: 停止cancal状态的任务（重复停止）
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务； 2、任务运行过程中停止任务； 3、重复多次停止该任务； 4、检查执行结果正确性；
 */
public class Transfer_stopSameTask439 extends TestScmBase {
    private static ScmId taskId = null;
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE = new Random().nextInt( 1024 ) + 1;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private int fileNum = 50;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private BSONObject cond = null;
    private String authorName = "StopSameTask439";

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + FILE_SIZE
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            rootSite = ScmInfo.getRootSite();
            branceSiteList = ScmInfo.getBranchSites( 2 );
            ws_T = ScmInfo.getWs();

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );
            session = TestScmTools.createSession( branceSiteList.get( 1 ) );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );
            prepareFiles( session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        ScmSession sessionM = null;
        ScmSession sessionA = null;
        ScmSession sessionB = null;
        try {
            sessionM = TestScmTools.createSession( rootSite );
            sessionA = TestScmTools.createSession( branceSiteList.get( 0 ) );
            sessionB = TestScmTools.createSession( branceSiteList.get( 1 ) );

            StopTaskThread stopTaskThreadM = new StopTaskThread( sessionM );
            StopTaskThread stopTaskThreadA = new StopTaskThread( sessionA );
            StopTaskThread stopTaskThreadB = new StopTaskThread( sessionB );

            startTask();
            waitTaskRunning();

            stopTaskThreadM.start();
            stopTaskThreadA.start();
            stopTaskThreadB.start();

            Assert.assertTrue( stopTaskThreadM.isSuccess(),
                    stopTaskThreadM.getErrorMsg() );
            Assert.assertTrue( stopTaskThreadA.isSuccess(),
                    stopTaskThreadA.getErrorMsg() );
            Assert.assertTrue( stopTaskThreadB.isSuccess(),
                    stopTaskThreadB.getErrorMsg() );

            waitTaskStop( taskId );

            checkTaskAttribute( session );
            List< ScmId > fileIdList = transferedFile();
            if ( fileIdList != null ) {
                checkTransContent( fileIdList );
            }

        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private void checkTransContent( List< ScmId > fileIdList )
            throws IOException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( ScmId fileId : fileIdList ) {
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.getContentFromLocalSite( downloadPath );
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            }
            SiteWrapper[] expSiteList = { rootSite, branceSiteList.get( 1 ) };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void startTask() {
        try {
            taskId = ScmSystem.Task.startTransferTask( ws, cond,
                    ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void waitTaskRunning() throws ScmException {
        Date startTime = null;
        while ( startTime == null ) {
            startTime = ScmSystem.Task.getTask( session, taskId )
                    .getStartTime();
        }
    }

    private void waitTaskStop( ScmId taskId ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private void checkTaskAttribute( ScmSession session ) throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( task.getId(), taskId );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), ws_T.getName() );
        Assert.assertEquals(
                task.getProgress() < 100 || task.getProgress() == 100, true );
    }

    private List< ScmId > transferedFile() {
        ScmSession session = null;
        List< ScmId > fileIdListed = new ArrayList< ScmId >();
        try {
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                int num = file.getLocationList().size();
                if ( num == 2 ) {
                    fileIdListed.add( fileId );
                }
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
        return fileIdListed;
    }

    private class StopTaskThread extends TestThreadBase {
        private ScmSession session;

        StopTaskThread( ScmSession session ) {
            this.session = session;
        }

        @Override
        public void exec() throws Exception {
            ScmSystem.Task.stopTask( session, taskId );
        }
    }
}
