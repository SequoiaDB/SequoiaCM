package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-166:输入流大小<1M（如1B、100K）
 * @author huangxiaoni init
 * @date 2017.3.29
 */

public class CreateAndGetScmFileByStream0B104 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile104";
    private ScmId fileId = null;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

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

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws IOException {
        testCreateScmFileByFile();
        testGetScmFileByFile();
        runSuccess = true;
    }

    private void testCreateScmFileByFile() throws IOException {
        InputStream ism = null;
        try {
            // create file
            ScmFile file = ScmFactory.File.createInstance( ws );
            ism = new FileInputStream( new File( filePath ) );
            file.setContent( ism );

            file.setFileName( fileName );
            file.setTitle( "sequoiacm" );
            file.setMimeType( MimeType.CSS );
            fileId = file.save();

            // check file's attribute
            checkFileAttributes( file );
        } catch ( ScmException | FileNotFoundException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( ism != null )
                ism.close();
        }
    }

    private void testGetScmFileByFile() {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            OutputStream fileOutputStream = new FileOutputStream(
                    new File( downloadPath ) );
            file.getContent( fileOutputStream );
            fileOutputStream.close();

            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
            // check attribute
            checkFileAttributes( file );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void checkFileAttributes( ScmFile file ) {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );

            Assert.assertEquals( file.getFileName(), fileName );
            Assert.assertEquals( file.getAuthor(), "" );
            Assert.assertEquals( file.getTitle(), "sequoiacm" );
            Assert.assertEquals( file.getMimeTypeEnum(), MimeType.CSS );
            Assert.assertEquals( file.getSize(), fileSize );

            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );

            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
        } catch ( BaseException e ) {
            throw e;
        }
    }

}