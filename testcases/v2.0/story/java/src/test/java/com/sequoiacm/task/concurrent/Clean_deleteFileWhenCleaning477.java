package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-477: “执行清理任务”过程中删除文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、A线程在分中心A创建清理任务； 2、“执行清理任务”过程中（调用ScmTask.getRunningFlag()接口获取任务状态为running）
 * B线程在分中心A删除该文件； 3、检查A、B线程执行结果正确性；
 */

public class Clean_deleteFileWhenCleaning477 extends TestScmBase {
    private final int fileSize = 1024 * 1024 * 5;
    private final int fileNum = 10;
    private final String authorName = "DeleteFileWhenCleaning477 ";
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String filePath = null;
    private File localPath = null;
    private ScmId taskId = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSiteList = ScmInfo.getBranchSites( 2 );
            ws_T = ScmInfo.getWs();

            sessionA = ScmSessionUtils.createSession( branceSiteList.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            writeFileOnA();
            readFileFromB();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = fileIdList.get( i );
                StartTask startTask = new StartTask( fileId );
                startTask.start();

                DeleteFile deleteFile = new DeleteFile( fileId );
                deleteFile.start();

                Assert.assertTrue( startTask.isSuccess(),
                        startTask.getErrorMsg() );
                Assert.assertTrue( deleteFile.isSuccess(),
                        deleteFile.getErrorMsg() );

                ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            }

            checkResult();
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
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void writeFileOnA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePath );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName + i );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void readFileFromB() throws Exception {
        ScmSession sessionB = null;
        try {
            // login
            sessionB = ScmSessionUtils.createSession( branceSiteList.get( 1 ) );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    sessionB );

            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = fileIdList.get( i );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.getContent( downloadPath );
            }
        } finally {
            if ( sessionB != null )
                sessionB.close();
        }
    }

    private void checkResult() {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( int i = 0; i < fileNum; i++ ) {
                BSONObject cond = new BasicBSONObject( "id",
                        fileIdList.get( i ).get() );
                long cnt = ScmFactory.File.countInstance( ws,
                        ScopeType.SCOPE_CURRENT, cond );
                Assert.assertEquals( cnt, 0 );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class StartTask extends TestThreadBase {
        private ScmId fileId;

        public StartTask( ScmId fileId ) {
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branceSiteList.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );

                // start task
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_ID )
                        .is( fileId.get() ).get();
                taskId = ScmSystem.Task.startCleanTask( ws, condition );

                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFile extends TestThreadBase {
        private ScmId fileId;

        public DeleteFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( branceSiteList.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );

                ScmFactory.File.deleteInstance( ws, fileId, true );
            } finally {
                if ( session != null )
                    session.close();
            }
        }
    }
}