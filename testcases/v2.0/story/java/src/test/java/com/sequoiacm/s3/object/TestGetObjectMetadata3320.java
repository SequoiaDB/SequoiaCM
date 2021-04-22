package com.sequoiacm.s3.object;

import java.util.HashMap;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3320：指定ifMatch和ifNoneMatch条件查询对象
 *
 * @author wangkexin
 * @Date 2018.12.17
 * @version 1.00
 */

public class TestGetObjectMetadata3320 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3320";
    private String keyName = "key3320";
    private String content = "content3320";
    private String eTag1 = null;
    private String eTag2 = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        PutObjectResult resultV1 = s3Client.putObject( bucketName, keyName,
                content + "v1" );
        eTag1 = resultV1.getETag();
        s3Client.putObject( bucketName, keyName, content + "v2" );
        PutObjectResult resultV3 = s3Client.putObject( bucketName, keyName,
                content + "v3" );
        eTag2 = resultV3.getETag();
    }

    @Test
    private void testGetObjectMetadata() throws Exception {
        // 指定ifMatch和ifNoneMatch匹配条件，ifMatch设置Etag值为当前版本对象的Etag值（v3版本），ifNoneMatch设置Etag值为历史版本对象的Etag值（如v1版本）
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request1.putCustomRequestHeader( "if-Match", eTag2 );
        request1.putCustomRequestHeader( "If-None-Match", eTag1 );
        ObjectMetadata objectMeta1 = s3Client.getObjectMetadata( request1 );
        Assert.assertEquals( objectMeta1.getETag(), eTag2 );
        Assert.assertEquals( objectMeta1.getUserMetadata(), new HashMap<>() );
        Assert.assertEquals( objectMeta1.getContentLength(),
                ( content + "v2" ).length() );
        Assert.assertNotNull( objectMeta1.getLastModified() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, keyName );
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
