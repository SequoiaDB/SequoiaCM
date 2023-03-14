package com.sequoiacm.scmfile;

import java.io.File;
import java.util.Date;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Testcase: SCM-131:指定ws和id获取文件实例，并获取文件详细信息 SCM-532:file.getLobId接口测试
 *            SCM-533:file.getLocationList接口测试
 * @author huangxiaoni init
 * @date 2017.4.7
 */

public class GetScmFileAttriByFieldId131 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile131";
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

    @Test(groups = { GroupTags.base })
    private void testGetScmFileAllAttriByFieldID() {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );

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
            Assert.assertEquals( file.getMimeTypeEnum(), MimeType.HTML );
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

            Assert.assertEquals( file.getDataId().get(),
                    file.getFileId().get() );

            int siteId = file.getLocationList().get( 0 ).getSiteId();
            Assert.assertEquals( siteId, site.getSiteId() );

            long accessTime = file.getLocationList().get( 0 ).getDate()
                    .getTime();
            if ( accessTime != file.getUpdateTime().getTime() ) {
                throw new Exception( "file.getLocationList is error. "
                        + "accessTime=" + accessTime + ", " + "createTime="
                        + file.getCreateTime().getTime() );
            }
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
            file.setMimeType( MimeType.HTML );

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