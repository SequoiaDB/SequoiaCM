package com.sequoiacm.auth;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3624:用户不存在，进行登录
 * @Author YiPan
 * @Date 2021/6/17
 */
public class S3AuthServer3624 extends TestScmBase {
    private AmazonS3 amazonS3 = null;
    private String accessKey = "test";
    private String secretKey = "test";
    private String bucketName = "bucket_3624";

    @BeforeClass
    private void setUp() {
    }

    @Test(enabled = false)
    private void test() throws Exception {
        amazonS3 = S3Utils.buildS3Client( accessKey, secretKey );
        try {
            amazonS3.doesBucketExistV2( bucketName );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 403 );
            Assert.assertEquals( e.getErrorCode(), "InvalidAccessKeyId" );
        }
    }

    @AfterClass
    private void tearDown() {
        amazonS3.shutdown();
    }
}
