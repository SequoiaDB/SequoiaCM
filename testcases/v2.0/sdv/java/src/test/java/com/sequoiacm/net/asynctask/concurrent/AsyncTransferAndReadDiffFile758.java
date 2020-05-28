package com.sequoiacm.net.asynctask.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-758 : 分中心A的2个节点并发异步迁移、读取不同文件
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、分中心A的2个节点并发异步迁移、读取不同文件； 2、检查执行结果正确性；
 */

public class AsyncTransferAndReadDiffFile758 extends TestScmBase {
    private final int fileSize = 64 * 1024 * 1024;
    private final int fileNum = 2;
    private boolean runSuccess = false;
    private File localPath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = "file758";
    private String filePath = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );

            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getSortSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            session = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            writeFileFromSubCenterA();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        AsyncTransferThread aThd = new AsyncTransferThread();
        ReadFileThread rThd = new ReadFileThread();
        aThd.start( 5 );
        rThd.start( 5 );

        Assert.assertTrue( aThd.isSuccess(), aThd.getErrorMsg() );
        Assert.assertTrue( rThd.isSuccess(), rThd.getErrorMsg() );

        checkAsyncTransfer();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( ws_T, cond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void writeFileFromSubCenterA() {
        try {
            for ( int i = 0; i < fileNum; ++i ) {
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setContent( filePath );
                scmfile.setFileName( author + "_" + UUID.randomUUID() );
                scmfile.setAuthor( author );
                fileIdList.add( scmfile.save() );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkAsyncTransfer() throws Exception {
        fileIdList.remove( 1 );
        SiteWrapper[] expSiteList = { sourceSite, targetSite };
        for ( ScmId fileId : fileIdList ) {
            ScmTaskUtils.waitAsyncTaskFinished( ws, fileId,
                    expSiteList.length );
        }
        ScmFileUtils.checkMetaAndData( ws_T, fileIdList.get( 0 ), expSiteList,
                localPath, filePath );
    }

    private class AsyncTransferThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmWorkspace ws = null;
            try {
                session = TestScmTools.createSession( sourceSite );
                ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                        session );
                ScmFactory.File.asyncTransfer( ws, fileIdList.get( 0 ) );
            } catch ( ScmException e ) {
                throw e;
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ReadFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmWorkspace ws = null;
            OutputStream fos = null;
            try {
                session = TestScmTools.createSession( sourceSite );
                ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                        session );
                ScmFile scmfile = ScmFactory.File.getInstance( ws,
                        fileIdList.get( 1 ) );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                scmfile.getContent( fos );

                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } catch ( ScmException e ) {
                throw e;
            } finally {
                if ( fos != null ) {
                    fos.close();
                }
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}