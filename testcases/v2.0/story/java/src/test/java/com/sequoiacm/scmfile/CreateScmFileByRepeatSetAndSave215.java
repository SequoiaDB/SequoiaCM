package com.sequoiacm.scmfile;

import java.io.File;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-215:写文件，重复设置属性并重复save
 * @author huangxiaoni init
 * @date 2017.4.13
 */

public class CreateScmFileByRepeatSetAndSave215 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile215";
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String filePath2 = null;
    private int fileSize = 10;
    private int fileSize2 = 100;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize2
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            TestTools.LocalFile.createFile( filePath2, fileSize2 );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        testCreateScmFileByFile();
        testGetScmFileByFile();
    }

    private void testCreateScmFileByFile() {
        try {
            // create file
            ScmFile file = ScmFactory.File.createInstance( ws );

            file.setContent( filePath );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setAuthor( "test" );
            file.setTitle( "sequoiacm" );
            file.setMimeType( "text/plain" );
            // repeat set
            file.setContent( filePath2 );
            file.setFileName( fileName + "_2" );
            file.setAuthor( "test2" );
            file.setTitle( "sequoiacm2" );
            file.setMimeType( "text/html" );

            fileId = file.save();

            // check results
            checkFileAttributes( file );
            runSuccess = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void testGetScmFileByFile() {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );

            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );

            // check results
            Assert.assertEquals( TestTools.getMD5( filePath2 ),
                    TestTools.getMD5( downloadPath ) );
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
            Assert.assertEquals( file.getFileName(), fileName + "_2" );
            Assert.assertEquals( file.getAuthor(), "test2" );
            Assert.assertEquals( file.getTitle(), "sequoiacm2" );
            Assert.assertEquals( file.getMimeType(), "text/html" );
            Assert.assertEquals( file.getSize(), fileSize2 );
        } catch ( BaseException e ) {
            throw e;
        }
    }

}