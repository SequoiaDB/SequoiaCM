package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
 * @FileName SCM-442:并发查询任务进度、停止任务
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务，迁移多个文件； 2、迁移文件过程中并发查询任务进度（循环多次查询）、停止任务； 3、检查返回结果正确性；
 */
public class Transfer_readProgressAndStop442 extends TestScmBase {
    private static ScmId taskId = null;
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE = 1024;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private int fileNum = 100;
    private String authorName = "ReadProgressAndStop442";
    private BSONObject cond = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

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
        try {
            StopTaskAndReadProgressThread TaskThreadM = new StopTaskAndReadProgressThread(
                    rootSite );
            StopTaskAndReadProgressThread TaskThreadA = new StopTaskAndReadProgressThread(
                    branceSiteList.get( 0 ) );
            StopTaskAndReadProgressThread TaskThreadB = new StopTaskAndReadProgressThread(
                    branceSiteList.get( 1 ) );

            startTask();
            waitTaskRunning();

            TaskThreadM.start();
            TaskThreadA.start();
            TaskThreadB.start();

            Assert.assertTrue( TaskThreadM.isSuccess(),
                    TaskThreadM.getErrorMsg() );
            Assert.assertTrue( TaskThreadA.isSuccess(),
                    TaskThreadA.getErrorMsg() );
            Assert.assertTrue( TaskThreadB.isSuccess(),
                    TaskThreadB.getErrorMsg() );

            waitTaskStop( taskId );

            checkTaskAttribute();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( ws_T, cond );
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

    private void checkTaskAttribute() throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( task.getId(), taskId );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), ws_T.getName() );
        Assert.assertEquals( task.getProgress() < 100, true );
    }

    private void waitTaskStop( ScmId taskId ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private class StopTaskAndReadProgressThread extends TestThreadBase {
        private SiteWrapper site;

        StopTaskAndReadProgressThread( SiteWrapper site ) {
            this.site = site;
        }

        @Override
        public void exec() throws Exception {
            ScmSession ss = null;
            try {
                ss = TestScmTools.createSession( site );
                ScmSystem.Task.stopTask( ss, taskId );
                ScmSystem.Task.getTask( ss, taskId ).getProgress();
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( null != ss ) {
                    ss.close();
                }
            }
        }
    }
}
