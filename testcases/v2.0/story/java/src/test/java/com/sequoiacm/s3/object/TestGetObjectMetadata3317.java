package com.sequoiacm.s3.object;

import java.util.HashMap;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3317：指定ifNoneMatch条件，不带versionId查询对象
 *
 * @author wangkexin
 * @Date 2018.12.10
 * @version 1.00
 */

public class TestGetObjectMetadata3317 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3317";
    private String keyName = "key3317";
    private String content = "content3317";
    private String eTag2 = null;
    private String eTag3 = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, content + "v1" );
        PutObjectResult resultV2 = s3Client.putObject( bucketName, keyName,
                content + "v2" );
        PutObjectResult resultV3 = s3Client.putObject( bucketName, keyName,
                content + "v3" );
        eTag2 = resultV2.getETag();
        eTag3 = resultV3.getETag();
    }

    @Test
    private void testGetObjectMetadata() throws Exception {
        // 不正确的eTag
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        System.out.println( "eTag3 = " + eTag3 );
        request1.putCustomRequestHeader( "If-None-Match", eTag2 );
        ObjectMetadata objectMeta1 = s3Client.getObjectMetadata( request1 );
        Assert.assertEquals( objectMeta1.getETag(), eTag3 );
        Assert.assertEquals( objectMeta1.getUserMetadata(), new HashMap<>() );
        Assert.assertEquals( objectMeta1.getContentLength(),
                ( content + "v2" ).length() );
        Assert.assertNotNull( objectMeta1.getLastModified() );

        // 正确的eTag
        GetObjectMetadataRequest request2 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request2.putCustomRequestHeader( "If-None-Match", eTag3 );
        try {
            s3Client.getObjectMetadata( request2 );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( AmazonS3Exception e ) {
            if ( e.getStatusCode() != 304 ) {
                throw e;
            }
        }
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
