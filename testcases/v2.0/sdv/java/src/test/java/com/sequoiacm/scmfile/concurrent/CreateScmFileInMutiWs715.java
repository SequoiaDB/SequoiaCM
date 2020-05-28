package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
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
import com.sequoiadb.exception.BaseException;

/**
 * @Description:SCM-715:多个ws下并发写文件 1、创建多个ws； 2、并发在多个ws下写文件； 3、检查并发写文件结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class CreateScmFileInMutiWs715 extends TestScmBase {
    private static final String author = "scmfile715";
    private final int wsNum = 3;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private List< WsWrapper > wsList = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            site = ScmInfo.getSite();
            wsList = ScmInfo.getWss( wsNum );

            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            WriteScmFile wThread1 = new WriteScmFile(
                    wsList.get( 0 ).getName() );
            wThread1.start();

            WriteScmFile wThread2 = new WriteScmFile(
                    wsList.get( 1 ).getName() );
            wThread2.start();

            WriteScmFile wThread3 = new WriteScmFile(
                    wsList.get( 2 ).getName() );
            wThread3.start();

            if ( !( wThread1.isSuccess() && wThread2.isSuccess()
                    && wThread3.isSuccess() ) ) {
                Assert.fail( wThread1.getErrorMsg() + wThread2.getErrorMsg()
                        + wThread3.getErrorMsg() );
            }

            checkResult( wsList.get( 0 ), wThread1.getFileId() );
            checkResult( wsList.get( 1 ), wThread2.getFileId() );
            checkResult( wsList.get( 2 ), wThread3.getFileId() );

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( wsList.get( 0 ), cond );
                ScmFileUtils.cleanFile( wsList.get( 1 ), cond );
                ScmFileUtils.cleanFile( wsList.get( 2 ), cond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkResult( WsWrapper wsp, ScmId fileId ) {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
            checkFileAttributes( file, fileId, wsp.getName() );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void checkFileAttributes( ScmFile file, ScmId fileId,
            String wsName ) {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsName );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
        } catch ( BaseException e ) {
            throw e;
        }
    }

    private class WriteScmFile extends TestThreadBase {
        private String wsName = null;
        private ScmId fileId = null;

        public WriteScmFile( String wsName ) {
            this.wsName = wsName;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                // write
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                setFileId( file.save() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        public ScmId getFileId() {
            return fileId;
        }

        public void setFileId( ScmId fileId ) {
            this.fileId = fileId;
        }
    }
}
