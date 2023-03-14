package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-5018:S3接口重复创建同名文件，桶更新版本控制，SCM API获取/更新/删除文件
 * @author wuyan
 * @Date 2022.07.23
 * @version 1.00
 */
public class Object5018 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5018";
    private String keyName = "object5018";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 3;
    private int updateSize = 1024;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmBucket scmBucket = null;
    private ScmId fileId = null;
    private long bucketId;

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

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        int createFileCount = 20;
        for ( int i = 0; i < createFileCount - 1; i++ ) {
            s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }

        // scm接口获取文件元数据和内容
        int currentVersion = -2;
        scmBucket = ScmFactory.Bucket.getBucket( session, bucketName );
        bucketId = scmBucket.getId();
        ScmFile file = scmBucket.getFile( keyName );
        fileId = file.getFileId();
        checkFileAttributes( file, currentVersion, fileSize, createFileCount );
        S3Utils.checkFileContent( file, filePath, localPath );

        // 开启版本控制，更新文件内容
        scmBucket.enableVersionControl();
        ScmFileUtils.createFile( scmBucket, keyName, updatePath );

        // 检查更新后属性，当前版本为更新文件，原文件为历史版本
        int newCurrentVersion = createFileCount + 1;
        ScmFile curfile = scmBucket.getFile( keyName );
        checkFileAttributes( curfile, newCurrentVersion, updateSize,
                newCurrentVersion );
        S3Utils.checkFileContent( curfile, updatePath, localPath );

        // 指定删除null版本（-2）
        int historyVersion = -2;
        scmBucket.deleteFileVersion( keyName, historyVersion, 0 );
        try {
            scmBucket.getFile( keyName, historyVersion, 0 );
            Assert.fail( "delete version should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "FILE_NOT_FOUND",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

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

    private void checkFileAttributes( ScmFile file, int fileVersion,
            long fileSize, int majorSerialVersion ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), keyName );
        Assert.assertEquals( file.getBucketId().longValue(), bucketId );

        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), fileVersion );
        Assert.assertEquals( file.getVersionSerial().getMajorSerial(),
                majorSerialVersion );
        if ( file.getMajorVersion() == -2 ) {
            Assert.assertTrue( file.isNullVersion() );
            Assert.assertEquals( file.getTitle(), "" );
        } else {
            Assert.assertFalse( file.isNullVersion() );
            Assert.assertEquals( file.getTitle(), keyName );
        }

    }
}
