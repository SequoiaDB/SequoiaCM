package com.sequoiacm.s3.object;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3311：doesObjectExist查询对象 
 *
 * @author wangkexin
 * @Date 2018.12.07
 * @version 1.00
 */

public class TestDoesObjectExist3311 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3311";
    private String keyName = "key3311";
    private String content = "content3311";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    private void testDoesObjectExist() throws Exception {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, content );
        Assert.assertTrue( s3Client.doesObjectExist( bucketName, keyName ) );
        s3Client.deleteObject( bucketName, keyName );
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, keyName ) );
        s3Client.putObject( bucketName, keyName, content );
        Assert.assertTrue( s3Client.doesObjectExist( bucketName, keyName ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        if ( runSuccess ) {
            s3Client.deleteObject( bucketName, keyName );
            s3Client.deleteBucket( bucketName );
        }
        if ( s3Client != null ) {
            s3Client.shutdown();
        }
    }
}
