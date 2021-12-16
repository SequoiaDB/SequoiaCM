package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-496: 并发在分中心迁移文件、删除文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步迁移单个文件、删除文件； 2、检查执行返回结果； 3、后台异步迁移任务执行完成后检查迁移后的文件正确性；
 */
public class AsyncTransferAndDeleteFile496 extends TestScmBase {
    private static final int fileNum = 50;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private int fileSize = 10;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "test496";
    private List< ScmId > fileList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() {

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmScheduleUtils.getSortBranchSites().get( 0 );
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // login in
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            writeFileFromSubCenterA( wsA, filePath );
        } catch ( ScmException | IOException e ) {
            Assert.fail( e.getMessage() );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        List< AsyncTransferFromSubCenterA > asyncTrList = new ArrayList<>();
        List< DeleteFromA > delList = new ArrayList<>();
        for ( int i = 0; i < fileNum; i++ ) {
            AsyncTransferFromSubCenterA asyncTransferAT = new AsyncTransferFromSubCenterA(
                    i );
            asyncTransferAT.start();
            asyncTrList.add( asyncTransferAT );

            DeleteFromA deleteFileAT = new DeleteFromA( i );
            deleteFileAT.start();
            delList.add( deleteFileAT );
        }

        for ( int i = 0; i < fileNum; i++ ) {
            AsyncTransferFromSubCenterA asyncTransferAT = asyncTrList.get( i );
            DeleteFromA deleteFileAT = delList.get( i );
            if ( !( asyncTransferAT.isSuccess()
                    && deleteFileAT.isSuccess() ) ) {
                Assert.fail( asyncTransferAT.getErrorMsg()
                        + deleteFileAT.getErrorMsg() );
            }
        }

        checkResult();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void writeFileFromSubCenterA( ScmWorkspace ws, String filePath )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile scmfile = ScmFactory.File.createInstance( wsA );
            scmfile.setContent( filePath );
            scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
            ScmId fileId = scmfile.save();
            fileList.add( fileId );
        }
    }

    private void checkResult() {
        try {
            for ( ScmId fileId : fileList ) {
                BSONObject cond = new BasicBSONObject( "id", fileId.get() );
                long cnt = ScmFactory.File.countInstance( wsA,
                        ScopeType.SCOPE_CURRENT, cond );
                Assert.assertEquals( cnt, 0 );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class AsyncTransferFromSubCenterA extends TestThreadBase {
        private int num;

        public AsyncTransferFromSubCenterA( int num ) {
            this.num = num;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmId fileId = fileList.get( num );
            try {
                session = TestScmTools.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFactory.File.asyncTransfer( ws, fileId,
                        rootSite.getSiteName() );
                int expSiteNum = 2;
                ScmTaskUtils.waitAsyncTaskFinished( ws, fileId, expSiteNum );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND
                        && e.getError() != ScmError.FILE_NOT_FOUND ) {
                    e.printStackTrace();
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFromA extends TestThreadBase {
        private int num;

        public DeleteFromA( int num ) {
            this.num = num;
        }

        @Override
        public void exec() throws ScmException {
            ScmSession session = null;
            ScmId fileId = fileList.get( num );
            try {
                session = TestScmTools.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
                // ScmFactory.File.deleteInstance(ws, fileId,
                // true);
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
