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
 * @Descreption SCM-3292:指定ifNoneMatch条件，不带versionId获取对象（标准模式）
 * @Author
 * @Date 2020/3/10
 */
public class GetObject3292 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3292";
    private String objectName = "object3292";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localfile" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectName, new File( filePath ) );
        // 获取文件etag
        String eTag = TestTools.getMD5( filePath );
        // 选择获取
        GetObjectRequest request = new GetObjectRequest( bucketName,
                objectName );
        // 设置正确eTag
        request.withNonmatchingETagConstraint( eTag );
        S3Object object = s3Client.getObject( request );
        Assert.assertNull( object );
        // 设置错误eTag
        request.withNonmatchingETagConstraint( "wrong" );
        object = s3Client.getObject( request );
        Assert.assertEquals( object.getKey(), objectName );

        // 下载校验
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        String acteTag = TestTools.getMD5( downloadPath );
        Assert.assertEquals( acteTag, eTag );
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
