package com.sequoiacm.task.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
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
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-433: “开始迁移任务”过程中修改文件属性满足迁移条件
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、A线程在分中心A创建迁移任务； 2、“开始迁移任务”过程中（任务状态为init，调用ScmTask.getRunningFlag()接口获取任务状态）
 * B线程修改该文件属性满足迁移条件（修改前不满足）； 3、检查A、B线程执行结果正确性； 备注：
 * 我不能确定有没有撞到Init阶段，因为没有特征。但这是我能想到能撞到的最好的办法了。
 */

public class Transfer_updateWhenTaskInit433 extends TestScmBase {
    private final int fileSize = 512 * 1024;
    private final int fileNum = 100;
    private final String transauthorName = "case433";
    private final String randauthorName = "case433_rand";
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionA = null;
    private ScmSession sessionM = null;

    private ScmWorkspace ws = null;
    private ScmId taskId = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();

        sessionM = TestScmTools.createSession( rootSite );
        sessionA = TestScmTools.createSession( branceSite );
        ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        prepareFiles( ws );
        bulkUpdateFileName( ws, fileNum / 2, fileNum, randauthorName );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        UpdateThread updateThd = new UpdateThread( fileNum / 2, fileNum,
                transauthorName );
        TransferThread transThd = new TransferThread();
        updateThd.start();
        transThd.start();

        Assert.assertTrue( updateThd.isSuccess(), updateThd.getErrorMsg() );
        Assert.assertTrue( transThd.isSuccess(), transThd.getErrorMsg() );

        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
        checkTransfered( 0, fileNum / 2 );
        checkFileUsable( 0, fileNum );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.getInstance( ws, fileIdList.get( i ) )
                            .delete( true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void bulkUpdateFileName( ScmWorkspace ws, int start, int end,
            String newFileName ) throws ScmException {
        for ( int i = start; i < end; ++i ) {
            ScmFile scmfile = ScmFactory.File.getInstance( ws,
                    fileIdList.get( i ) );
            scmfile.setFileName( newFileName + "_" + i + UUID.randomUUID() );
        }
    }

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( transauthorName + "_" + i );
            scmfile.setAuthor( transauthorName );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private void checkTransfered( int start, int end ) {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( start, end ), expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkFileUsable( int start, int end ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        ScmSession session = null;
        try {
            // login
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );

            // read content
            for ( int i = start; i < end; ++i ) {
                ScmId fileId = fileIdList.get( i );
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );

                // check content on main center
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            }
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class TransferThread extends TestThreadBase {
        private ScmSession sessionA = null;
        private ScmWorkspace ws = null;

        public TransferThread() throws ScmException {
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        }

        @Override
        public void exec() throws Exception {
            try {
                BSONObject condition = new BasicBSONObject(
                        ScmAttributeName.File.AUTHOR, transauthorName );
                taskId = ScmSystem.Task.startTransferTask( ws, condition,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        rootSite.getSiteName() );
                ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

    private class UpdateThread extends TestThreadBase {
        private int start;
        private int end;
        private String newFileName;
        private ScmSession sessionA;
        private ScmWorkspace ws = null;

        public UpdateThread( int start, int end, String newFileName )
                throws ScmException {
            this.start = start;
            this.end = end;
            this.newFileName = newFileName;
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        }

        @Override
        public void exec() throws Exception {
            try {
                for ( int i = start; i < end; ++i ) {
                    ScmFile scmfile = ScmFactory.File.getInstance( ws,
                            fileIdList.get( i ) );
                    scmfile.setFileName( newFileName + "_" + i );
                    scmfile.setAuthor( newFileName );
                }
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }
}