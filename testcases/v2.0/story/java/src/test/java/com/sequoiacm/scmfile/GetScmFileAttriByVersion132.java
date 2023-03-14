package com.sequoiacm.scmfile;

import java.io.File;
import java.util.Date;

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
import com.sequoiacm.common.MimeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-132:指定版本号获取文件实例，并获取文件信息
 * @author huangxiaoni init
 * @date 2017.4.7
 */

public class GetScmFileAttriByVersion132 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile132";
    private String author = fileName;
    private ScmId fileId = null;
    private ScmId fileId2 = null;
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
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            fileId = this.createScmFile( ws, filePath, 0 );
            fileId2 = this.createScmFile( ws, filePath, 1 );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void testGetScmFileAllAttriByVersion() {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId, 1, 0 );

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

            Assert.assertEquals( file.getFileName(), fileName + "_0" );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getTitle(), "sequoiacm0" );

            Assert.assertEquals( file.getMimeTypeEnum(), MimeType.CSS );
            Assert.assertEquals( file.getMimeType(), MimeType.CSS.getType() );
            Assert.assertEquals( file.getMimeTypeEnum().getSuffix(),
                    MimeType.CSS.getSuffix() );

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
            Assert.assertEquals( file.getUpdateTime(), file.getCreateTime() );
            // Assert.assertEquals(file.getPropertyType(), PropertyType.VIDEO);
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
                ScmFactory.File.deleteInstance( ws, fileId2, true );
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

    private ScmId createScmFile( ScmWorkspace ws, String filePath, int i ) {
        ScmId scmFileID = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );

            file.setContent( filePath );
            file.setFileName( fileName + "_" + i );
            file.setTitle( "sequoiacm" + i );
            file.setAuthor( author );
            file.setMimeType( MimeType.CSS );

            // file.setPropertyType(PropertyType.VIDEO);

            scmFileID = file.save();

            if ( i == 0 ) {
                localTime = new Date().getTime();
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        return scmFileID;
    }

}