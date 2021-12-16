package com.sequoiacm.task.concurrent;

import java.io.File;
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
import com.sequoiacm.client.exception.ScmException;
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
 * @FileName SCM-441: 并发查询任务进度
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务，迁移多个文件； 2、迁移文件过程中多线程并发查询任务进度； 3、检查返回结果正确性；
 */
public class Transfer_readTaskProgress441 extends TestScmBase {

    private static ScmId taskId = null;
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE = new Random().nextInt( 100 ) + 1024;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private int fileNum = 100;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private BSONObject cond = null;
    private String authorName = "ReadTaskProgress441";

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

            session = TestScmTools.createSession( branceSiteList.get( 0 ) );
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

            ReadTaskProgressThread TaskThreadM = new ReadTaskProgressThread(
                    sessionM );
            ReadTaskProgressThread TaskThreadA = new ReadTaskProgressThread(
                    sessionA );
            ReadTaskProgressThread TaskThreadB = new ReadTaskProgressThread(
                    sessionB );

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

            waitTaskStop();

            Assert.assertEquals( TaskThreadM.getProgress() < 100
                    || TaskThreadM.getProgress() == 100, true );
            Assert.assertEquals( TaskThreadA.getProgress() < 100
                    || TaskThreadM.getProgress() == 100, true );
            Assert.assertEquals( TaskThreadB.getProgress() < 100
                    || TaskThreadM.getProgress() == 100, true );
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

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private class ReadTaskProgressThread extends TestThreadBase {
        private ScmSession session;
        private int progress;

        ReadTaskProgressThread( ScmSession session ) {
            this.session = session;
        }

        @Override
        public void exec() throws Exception {
            progress = ScmSystem.Task.getTask( session, taskId ).getProgress();
        }

        public int getProgress() {
            return progress;
        }
    }
}
