package com.sequoiacm.s3.object;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3287:指定桶不存在
 * @author fanyu
 * @Date 2018.11.13
 * @version 1.00
 */
public class CreateObject3287 extends TestScmBase {
    private String non_existent_bucketName = "bucket3287";
    private String keyName = "aa/bb/object3287.png";
    private AmazonS3 s3Client = null;
    private String expContent = "file3287";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void testPutObject() throws Exception {
        // put object in a nonexistent bucket.
        try {
            s3Client.putObject( non_existent_bucketName, keyName, expContent );
            Assert.fail( "exp fail but found success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    @AfterClass
    private void tearDown() {
        if ( s3Client != null ) {
            s3Client.shutdown();
        }
    }
}
