package com.sequoiacm.s3.object;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3312：doesObjectExist查询指定桶不存在
 * @author fanyu
 * @Date 2018.12.07
 * @version 1.00
 */

public class TestDoesObjectExist3312 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3312";
    private String nonexistBucketName = "nonexistbucket3312";
    private String keyName = "key3312";
    private String content = "content3312";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    private void testDoesObjectExist() throws Exception {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, content );

        // the specified bucket name does not exist
        Assert.assertFalse(
                s3Client.doesObjectExist( nonexistBucketName, keyName ) );
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
