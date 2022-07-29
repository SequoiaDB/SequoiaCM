package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4801 :: S3接口创建版本对象，SCM API更新版本文件
 * @author wuyan
 * @Date 2022.07.15
 * @version 1.00
 */
public class Object4801 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4801";
    private String keyName = "object对象4801";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
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
        session = TestScmTools.createSession( site );
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void testCreateObject() throws Exception {
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        fileId = S3Utils.createFile( scmBucket, keyName, updatePath );

        // S3接口读取当前最新版本文件属性和内容
        String currentVersion = "2.0";
        String fileMd5_cur = TestTools.getMD5( updatePath );
        S3Object objectInfo = s3Client.getObject( bucketName, keyName );
        checkObjectAttributeInfo( objectInfo, currentVersion, updateSize,
                fileMd5_cur );
        // 检查对象内容
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, fileMd5_cur );

        // SCM API获取历史版本属性信息和内容
        int historyVersion = 1;
        ScmFile file1 = scmBucket.getFile( keyName, historyVersion, 0 );
        checkFileAttributes( file1, historyVersion, fileSize );
        S3Utils.checkFileContent( file1, filePath, localPath );
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

    private void checkFileAttributes( ScmFile file, int version, long size ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), keyName );
        Assert.assertEquals( file.getSize(), size );
        Assert.assertEquals( file.getBucketId().longValue(),
                scmBucket.getId() );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertFalse( file.isNullVersion() );
        Assert.assertEquals( file.getFileId(), fileId );
    }

    private void checkObjectAttributeInfo( S3Object objectInfo, String version,
            long size, String expMd5 ) {
        Assert.assertEquals( objectInfo.getBucketName(), bucketName );
        ObjectMetadata objectMeta = objectInfo.getObjectMetadata();
        Assert.assertEquals( objectMeta.getVersionId(), version );
        Assert.assertEquals( objectMeta.getContentLength(), size );
        Assert.assertEquals( objectMeta.getETag(), expMd5 );
    }
}
