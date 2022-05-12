package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Descreption SCM-3291:指定if-match条件，不带versionId获取对象（标准模式）
 * @Author YiPan
 * @Date 2021/3/10
 */
public class GetObject3291 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3291";
    private String objectName1 = "object3291A";
    private String objectName2 = "object3291b";
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private int fileSize = 1024 * 100;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localfileA" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localfileB" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectName1, new File( filePath1 ) );
        s3Client.putObject( bucketName, objectName2, new File( filePath2 ) );
        // 获取文件etag
        String eTag1 = TestTools.getMD5( filePath1 );
        String eTag2 = TestTools.getMD5( filePath2 );
        // 选择获取
        GetObjectRequest request = new GetObjectRequest( bucketName,
                objectName1 );
        request.withMatchingETagConstraint( eTag1 );
        S3Object object = s3Client.getObject( request );

        // 正确estag
        Assert.assertEquals( objectName1, object.getKey() );
        // 下载校验
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        String acteTag = TestTools.getMD5( downloadPath );
        Assert.assertEquals( acteTag, eTag1 );

        // 错误estag
        request.withMatchingETagConstraint( eTag2 );
        object = s3Client.getObject( request );
        Assert.assertNull( object );
    }

    @AfterClass
    private void tearDown() {
        try {
            S3Utils.clearBucket( s3Client, bucketName );
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            s3Client.shutdown();
        }
    }
}
