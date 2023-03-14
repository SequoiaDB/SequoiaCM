package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4804 :: S3接口创建多版本文件，SCM API指定版本删除版本文件
 * @author wuyan
 * @Date 2022.07.18
 * @version 1.00
 */
public class Object4804 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4804";
    private String keyName = "object4804";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 3;
    private int updateSize = 1024;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmId fileId = null;

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
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void test() throws Exception {
        int currentVersion = 3;
        int historyVersion1 = 2;
        int historyVersion2 = 1;
        ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                bucketName );

        // 场景a：指定最新版本删除
        scmBucket.deleteFileVersion( keyName, currentVersion, 0 );
        // 获取删除当前版本已不存在
        try {
            scmBucket.getFile( keyName, currentVersion, 0 );
            Assert.fail( "get file with currentVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取当前版本文件为原v2版本文件
        ScmFile file = scmBucket.getFile( keyName );
        Assert.assertEquals( file.getMajorVersion(), historyVersion1 );
        Assert.assertEquals( file.getSize(), updateSize );
        S3Utils.checkFileContent( file, updatePath, localPath );

        // 获取原v1版本文件未删除
        ScmFile file1 = scmBucket.getFile( keyName, historyVersion2, 0 );
        Assert.assertEquals( file1.getSize(), fileSize );

        // 场景 b：指定历史版本删除
        scmBucket.deleteFileVersion( keyName, historyVersion2, 0 );
        // 获取删除历史版本v1已不存在
        try {
            scmBucket.getFile( keyName, historyVersion2, 0 );
            Assert.fail( "get file with historyVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 校验最新版本对象还存在未删除
        ScmFile file3 = scmBucket.getFile( keyName );
        Assert.assertEquals( file3.getMajorVersion(), historyVersion1 );
        Assert.assertEquals( file3.getSize(), updateSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }
}
