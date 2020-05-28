package com.sequoiacm.scmfile;

import java.io.File;
import java.util.Date;
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
 * @Testcase: SCM-135:设置已存在的属性
 * @author huangxiaoni init
 * @date 2017.4.7
 */

public class UpdateScmFileAttri135 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile135";
    private ScmId fileId = null;
    private int fileSize = 1024 * 10;
    private long localTime; // local time when createScmFile
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

            fileId = this.createScmFile( ws, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testUpdateScmFileAttributes() {
        String author = "newAuthor";
        String name = fileName + "_" + UUID.randomUUID();
        String mimeType = "video/avi";
        String title = "newTitle";
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );

            // update file's attributes
            // file.setContent(is);
            file.setAuthor( author );
            file.setFileName( name );
            file.setMimeType( mimeType );
            file.setTitle( title );
            // file.setProperties(null);
            // file.setPropertyType(PropertyType.VIDEO);

            // get file's content, and check results
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );

            // get file's attributes, and check results
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getTitle(), title );
            Assert.assertEquals( file.getMimeType(), mimeType );
            Assert.assertEquals( file.getSize(), fileSize );

            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );

            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            long acceptableOffSet = 2000 * 1000; // unit:ms
            if ( Math.abs( file.getCreateTime().getTime()
                    - localTime ) > acceptableOffSet ) {
                Assert.fail( "time is different: scmCreateFullTime="
                        + file.getCreateTime().getTime() + ", localFullTime="
                        + localTime );
            }

            Assert.assertEquals( file.getUpdateUser(), file.getUser() );
            long createTime = file.getCreateTime().getTime();
            long updateTime = file.getUpdateTime().getTime();
            if ( ( updateTime - createTime ) > acceptableOffSet ) {
                Assert.fail( "time is different: scmCreateFullTime="
                        + createTime + ", updateFullTime=" + updateTime );
            }

            // Assert.assertEquals(file.getPropertyType(), PropertyType.VIDEO);
            runSuccess = true;
        } catch ( Exception e ) {
            e.printStackTrace();
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

    private ScmId createScmFile( ScmWorkspace ws, String filePath ) {
        ScmId scmFileID = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );

            file.setContent( filePath );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setTitle( "sequoiacm" );
            file.setAuthor( "admin" );
            file.setMimeType( "text/plain" );

            // file.setPropertyType(PropertyType.VIDEO);

            scmFileID = file.save();
            localTime = new Date().getTime();
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        return scmFileID;
    }

}