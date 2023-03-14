package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
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
 * @Description SCM-4837 :: 开启版本控制，历史版本为删除标记版本，带版本号删除最新版本文件
 * @author wuyan
 * @Date 2022.07.23
 * @version 1.00
 */
public class ScmFile4837 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4837";
    private String fileName = "scmfile4837";
    private SiteWrapper site = null;
    private ScmId fileId = null;
    private int fileSize = 1024 * 300;
    private int updateSize = 1024 * 128;
    private String filePath = null;
    private String updatePath = null;
    private File localPath = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        scmBucket.deleteFile( fileName, false );
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filePath );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        int currentVersion = 2;
        scmBucket.deleteFileVersion( fileName, currentVersion, 0 );
        try {
            scmBucket.getFile( fileName, currentVersion, 0 );
            Assert.fail( "get file with currentVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取当前版本为删除标记
        int newVersion = 1;
        checkFileVersion( newVersion );
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

    private void checkFileVersion( int version ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int fileVersion = file.getMajorVersion();
            Assert.assertEquals( file.getFileId(), fileId,
                    "---error file version is " + fileVersion );
            Assert.assertEquals( file.getMajorVersion(), version );
            Assert.assertTrue( file.isDeleteMarker() );
            Assert.assertFalse( file.isNullVersion() );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 1;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
