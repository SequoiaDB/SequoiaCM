package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4758 :: 不指定版本号获取删除标记的文件;SCM-4759 ::指定版本号获取带deleteMarker标记的文件
 * @author wuyan
 * @Date 2022.07.08
 * @version 1.00
 */
public class ScmFile4758_4759 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4758";
    private String fileName = "scmfile4758";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024;
    private ScmWorkspace ws = null;
    private File localPath = null;
    private String filePath = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();

        // 两次删除文件，当前版本和历史版本都存在删除标记文件
        scmBucket.deleteFile( fileName, false );
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filePath );
        scmBucket.deleteFile( fileName, false );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // test4757：不指定版本获取删除标记对象
        try {
            scmBucket.getFile( fileName );
            Assert.fail( "get file with deleteMarker should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // test4758：指定当前版本获取删除标记对象
        int currentVersion = 3;
        try {
            ScmFactory.File.getInstance( ws, fileId, currentVersion, 0 );
            Assert.fail( "get file with deleteMarker should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }
        // test4758：指定历史版本获取删除标记对象
        int historyVersion = 1;
        try {
            scmBucket.getFile( fileName, historyVersion, 0 );
            Assert.fail( "get file with deleteMarker should be failed!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 检查删除标记版本和非删除标记版本信息
        checkFileVersion();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileVersion() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        int existFileVersion = 2;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int fileVersion = file.getMajorVersion();
            if ( fileVersion == existFileVersion ) {
                Assert.assertFalse( file.isDeleteMarker() );
            } else {
                Assert.assertTrue( file.isDeleteMarker() );
            }
            Assert.assertEquals( file.getFileName(), fileName );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 3;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
