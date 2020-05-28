package com.sequoiacm.scmfile;

import java.io.File;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-128:获取指定查询条件下文件列表
 * @author huangxiaoni init
 * @date 2017.4.6
 */

public class ListInstanceByCondition128 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile128";
    private ScmId fileId = null;
    private ScmId fileId2 = null;
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

            fileId = this.createScmFile( ws, filePath, 0 );
            fileId2 = this.createScmFile( ws, filePath, 1 );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testListInstanceByAllAttributes() {
        String name = fileName + "_0";
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            // set condition
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            BSONObject condition = new BasicBSONObject();

            condition = new BasicBSONObject();
            condition.put( ScmAttributeName.File.FILE_NAME,
                    file.getFileName() );
            condition.put( ScmAttributeName.File.AUTHOR, file.getAuthor() );
            condition.put( ScmAttributeName.File.TITLE, file.getTitle() );
            condition.put( ScmAttributeName.File.MIME_TYPE,
                    file.getMimeType() );
            condition.put( ScmAttributeName.File.FILE_ID,
                    file.getFileId().get() );
            condition.put( ScmAttributeName.File.MAJOR_VERSION,
                    file.getMajorVersion() );
            condition.put( ScmAttributeName.File.MINOR_VERSION,
                    file.getMinorVersion() );
            condition.put( ScmAttributeName.File.USER, file.getUser() );
            condition.put( ScmAttributeName.File.CREATE_TIME,
                    file.getCreateTime().getTime() );
            condition.put( ScmAttributeName.File.UPDATE_USER,
                    file.getUpdateUser() );
            condition.put( ScmAttributeName.File.UPDATE_TIME,
                    file.getCreateTime().getTime() );
            condition.put( ScmAttributeName.File.SIZE, file.getSize() );

            // listInstance
            ScopeType scopeType = ScopeType.SCOPE_CURRENT;
            cursor = ScmFactory.File.listInstance( ws, scopeType, condition );
            int size = 0;
            ScmFileBasicInfo fileInfo;
            while ( cursor.hasNext() ) {
                fileInfo = cursor.getNext();
                // check results
                Assert.assertEquals( fileInfo.getFileName(), name );
                Assert.assertEquals( fileInfo.getFileId(), fileId,
                        fileInfo.toString() );
                Assert.assertEquals( fileInfo.getMinorVersion(), 0 );
                Assert.assertEquals( fileInfo.getMajorVersion(), 1 );

                Assert.assertEquals( fileInfo.getUser(),
                        TestScmBase.scmUserName );
                Assert.assertNotNull( fileInfo.getCreateDate() );
                Assert.assertEquals( fileInfo.getCreateDate(),
                        file.getDataCreateTime() );
                Assert.assertEquals( fileInfo.getMimeType(),
                        MimeType.CSS.getType() );
                size++;
            }
            Assert.assertEquals( size, 1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
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
            file.setAuthor( "admin" + i );
            file.setMimeType( MimeType.CSS );
            scmFileID = file.save();
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        return scmFileID;
    }

}