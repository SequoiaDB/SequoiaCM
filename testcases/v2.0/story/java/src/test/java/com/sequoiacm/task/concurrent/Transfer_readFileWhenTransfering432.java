package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.Date;
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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-432:“执行迁移任务”过程中查询文件
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、A线程在分中心A创建迁移任务；
 * 2、“执行迁移任务”过程中（调用ScmTask.getRunningFlag()接口获取任务状态为running）B线程查询该文件信息；
 * 3、检查A、B线程执行结果正确性；
 */
public class Transfer_readFileWhenTransfering432 extends TestScmBase {
    private static ScmId taskId = null;
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private String authorName = "transfer432";
    private ScmId fileId = null;
    private int FILE_SIZE = 1024 * 1024 * 9;
    private File localPath = null;
    private String filePath = null;
    private BSONObject cond = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            rootSite = ScmInfo.getRootSite();
            branSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            this.createFile();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })

    private void test() throws Exception {
        StartTaskThreadA startTask = new StartTaskThreadA();
        GetFileAttriThreadB getFileAttri = new GetFileAttriThreadB();
        startTask.start();
        getFileAttri.start( 30 );

        Assert.assertTrue( startTask.isSuccess(), startTask.getErrorMsg() );
        Assert.assertTrue( getFileAttri.isSuccess(),
                getFileAttri.getErrorMsg() );

        ScmTaskUtils.waitTaskFinish( sessionA, taskId );

        checkResult();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( ws_T, cond );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void waitTaskRunning() throws ScmException {
        Date startTime = null;
        while ( startTime == null ) {
            startTime = ScmSystem.Task.getTask( sessionA, taskId )
                    .getStartTime();
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( wsA );
        file.setContent( filePath );
        file.setFileName( authorName + "_" + UUID.randomUUID() );
        file.setAuthor( authorName );
        file.setMimeType( authorName );
        file.setTitle( authorName );
        file.setAuthor( authorName );
        fileId = file.save();
    }

    private class StartTaskThreadA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            try {
                taskId = ScmSystem.Task.startTransferTask( wsA, cond );
                waitTaskRunning();
            } catch ( ScmException e ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class GetFileAttriThreadB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                // read
                ScmFactory.File.getInstance( ws, fileId );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                Assert.assertEquals( file.getFileId().get(), fileId.get() );
                Assert.assertEquals( file.getAuthor(), authorName );
                Assert.assertEquals( file.getMimeType(), authorName );
                Assert.assertEquals( file.getTitle(), authorName );
                Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
                Assert.assertEquals( file.getUpdateUser(),
                        TestScmBase.scmUserName );
                Assert.assertEquals( file.getSize(), FILE_SIZE );
                Assert.assertEquals( file.getMajorVersion(), 1 );
                Assert.assertEquals( file.getMinorVersion(), 0 );
            } finally {
                if ( null != session )
                    session.close();
            }
        }
    }
}
