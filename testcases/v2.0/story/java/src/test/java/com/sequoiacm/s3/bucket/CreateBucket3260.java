package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @Descreption SCM-3260:指定桶名创建桶（标准模式）
 * @Author YiPan
 * @Date 2020/3/5
 */
public class CreateBucket3260 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3260";
    private String objectName = "3260f.txt";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectName, "test" );
        // 查看所有桶
        boolean flag = false;
        List< Bucket > buckets = s3Client.listBuckets();
        for ( Bucket s : buckets ) {
            Assert.assertNotEquals( s.getCreationDate(), null );
            Assert.assertEquals( s.getOwner().getDisplayName(),
                    TestScmBase.scmUserName );
            Assert.assertEquals( s.getOwner().getId(),
                    TestScmBase.scmUserName );
            if ( s.getName().equals( bucketName ) ) {
                flag = true;
                break;
            }
        }
        Assert.assertTrue( flag );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
            s3Client.shutdown();
        }
    }
}
