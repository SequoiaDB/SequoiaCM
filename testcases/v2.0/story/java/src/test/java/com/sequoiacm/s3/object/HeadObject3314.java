package com.sequoiacm.s3.object;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3314: head object and specify the bucket does not exist.
 * @author wuyan
 * @Date 2018.12.17
 * @version 1.00
 */
public class HeadObject3314 extends TestScmBase {
    private String bucketName = "bucket3314";
    private String key = "test/test2/object3314";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws IOException {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void testCreateBucket() {
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                bucketName, key );
        try {
            s3Client.getObjectMetadata( request );
            Assert.fail( "head object must be fail!" );
        } catch ( AmazonS3Exception e ) {
            // return 404 Not found
            Assert.assertEquals( e.getStatusCode(), 404 );
        }
    }

    @AfterClass
    private void tearDown() {
        s3Client.shutdown();
    }
}
