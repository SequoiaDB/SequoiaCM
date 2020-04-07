package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-435: “开始迁移任务”过程中删除文件
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、A线程在分中心A创建迁移任务； 2、“开始迁移任务”过程中（任务状态为init，调用ScmTask.getRunningFlag()接口获取任务状态）
 * B线程删除该文件； 3、检查A、B线程执行结果正确性；
 * 
 * 备注：大部分时候撞上的还是running状态。但我想不到办法保证撞上Init状态。
 */

public class Transfer_deleteWhenTaskInit435 extends TestScmBase {
    private boolean runSuccess = false;

    private int fileSize = 2 * 1024 * 1024;
    private int fileNum = 100;
    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private ScmId taskId = null;
    private String authorName = "DeleteWhenTaskInit435";

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        ws_T = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionM = TestScmTools.createSession( targetSite );
        sessionA = TestScmTools.createSession( sourceSite );
        ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

        this.prepareFiles( ws );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            DeleteThread deleteThd = new DeleteThread();
            TransferThread transThd = new TransferThread();
            deleteThd.start();
            transThd.start();
            Assert.assertTrue( deleteThd.isSuccess(), deleteThd.getErrorMsg() );
            Assert.assertTrue( transThd.isSuccess(), transThd.getErrorMsg() );
            try {
                // TODO: 更好的解决方法应该是根据元数据确认那些文件没有删除，然后检查没删除的文件的可用性。迁移也是。
                checkFileUsable();
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                    throw e;
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            throw e;
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId fileId : fileIdList ) {
                    try {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    } catch ( ScmException e ) {
                        if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                            throw e;
                        }
                    }
                }
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( BaseException | ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareFiles( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileIdList.add( scmfile.save() );
        }
    }

    private void checkFileUsable() throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        ScmSession session = null;
        try {
            // login
            session = TestScmTools.createSession( targetSite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), session );

            // read content
            for ( int i = 0; i < fileIdList.size(); ++i ) {
                ScmId fileId = fileIdList.get( i );
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );

                // check content on main center
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            }
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
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
            sessionA = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        }

        @Override
        public void exec() throws Exception {
            try {
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                        .get();
                taskId = ScmSystem.Task.startTransferTask( ws, condition,
                        ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );
                waitTaskStop();
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }

        private void waitTaskStop() throws ScmException {
            Date stopTime = null;
            while ( stopTime == null ) {
                stopTime = ScmSystem.Task.getTask( sessionA, taskId )
                        .getStopTime();
            }
        }
    }

    private class DeleteThread extends TestThreadBase {
        private ScmSession sessionA = null;
        private ScmWorkspace ws = null;

        public DeleteThread() throws ScmException {
            sessionA = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        }

        @Override
        public void exec() throws Exception {
            try {
                int len = fileIdList.size();
                for ( int i = len - 1; i >= 0; --i ) {
                    ScmFactory.File
                            .deleteInstance( ws, fileIdList.get( i ), true );
                }
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

}