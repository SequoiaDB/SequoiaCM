package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3309:指定桶不存在（标准模式）
 * @Author YiPan
 * @Date 2020/3/12
 */
public class GetObject3309 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3309";
    private String objectName = "object3309";
    private String unexistbucketName = "inexistence3309";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        s3Client.putObject( bucketName, objectName, "1234" );
        try {
            s3Client.getObject( unexistbucketName, objectName );
            Assert.fail( "exp fail but act success" );
        } catch ( AmazonS3Exception e ) {
            if ( e.getStatusCode() != 404 ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

}
