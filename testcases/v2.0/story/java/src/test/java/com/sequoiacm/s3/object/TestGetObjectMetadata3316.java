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
 * @Description: SCM-3316：匹配if-match条件，不带versionId查询对象
 * @author wangkexin
 * @Date 2018.12.07
 * @version 1.00
 */

public class TestGetObjectMetadata3316 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3316";
    private String keyName = "key3316";
    private String content = "content3316";
    private String eTag1 = null;
    private String eTag2 = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        PutObjectResult resultV1 = s3Client.putObject( bucketName, keyName,
                content + "v1" );
        PutObjectResult resultV2 = s3Client.putObject( bucketName, keyName,
                content + "v2" );
        eTag1 = resultV1.getETag();
        eTag2 = resultV2.getETag();
    }

    @Test
    private void testGetObjectMetadata() throws Exception {
        // 正确的eTag
        GetObjectMetadataRequest request1 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request1.putCustomRequestHeader( "if-Match", eTag2 );
        ObjectMetadata objectMeta1 = s3Client.getObjectMetadata( request1 );
        Assert.assertEquals( objectMeta1.getETag(), eTag2 );
        Assert.assertEquals( objectMeta1.getUserMetadata(), new HashMap<>() );
        Assert.assertEquals( objectMeta1.getContentLength(),
                ( content + "v2" ).length() );
        Assert.assertNotNull( objectMeta1.getLastModified() );

        // 不正确的eTag
        GetObjectMetadataRequest request2 = new GetObjectMetadataRequest(
                bucketName, keyName );
        request2.putCustomRequestHeader( "if-Match", eTag1 );
        try {
            s3Client.getObjectMetadata( request2 );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( AmazonS3Exception e ) {
            if ( e.getStatusCode() != 412 ) {
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
