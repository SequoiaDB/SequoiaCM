package com.sequoiacm.scmfile;

import java.io.File;
import java.util.UUID;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-218:重复更新相同属性
 * @author huangxiaoni init
 * @date 2017.4.13
 */

public class UpdateScmFileAttriRepeat218 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile218";
    private ScmId fileId = null;
    private int fileSize = 1024 * 10;
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
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            this.createScmFile( ws, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testUpdateScmFileAttributes() {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );

            // update file's attributes
            file.setFileName( fileName + "_1" );
            Assert.assertEquals( file.getFileName(), fileName + "_1" );
            file.setFileName( fileName + "_2" );
            Assert.assertEquals( file.getFileName(), fileName + "_2" );

            file.setTitle( "title1" );
            Assert.assertEquals( file.getTitle(), "title1" );
            file.setTitle( "title2" );
            Assert.assertEquals( file.getTitle(), "title2" );

            file.setMimeType( "text/plain" );
            Assert.assertEquals( file.getMimeType(), "text/plain" );
            file.setMimeType( "audio/x-aiff" );
            Assert.assertEquals( file.getMimeType(), "audio/x-aiff" );

            file.setAuthor( "author1" );
            Assert.assertEquals( file.getAuthor(), "author1" );
            file.setAuthor( "author2" );
            Assert.assertEquals( file.getAuthor(), "author2" );

            // get file's content, and check results
            ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file2.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
            // check attributes
            Assert.assertEquals( file.getFileName(), fileName + "_2" );
            Assert.assertEquals( file.getTitle(), "title2" );
            Assert.assertEquals( file.getMimeType(), "audio/x-aiff" );
            Assert.assertEquals( file.getAuthor(), "author2" );

        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
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

    private void createScmFile( ScmWorkspace ws, String filePath ) {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setTitle( "sequoiacm" );
            file.setAuthor( "admin" );
            file.setMimeType( "text/plain" );
            fileId = file.save();
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

}