package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3278:并发创建和删除相同桶
 * @Author YiPan
 * @Date 2021/3/6
 */
public class CreateAndDropBucket3278 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3278";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        CreateBucket t1 = new CreateBucket();
        DropBucket t2 = new DropBucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();
        if ( !( ( t1.result + t2.result ).equals( "" ) ) ) {
            Assert.assertEquals( t1.result + t2.result, "NoSuchBucket" );
        } else {
            Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }
        s3Client.shutdown();
    }

    class CreateBucket {
        public String result = "";

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                AmazonS3 amazonS3 = S3Utils.buildS3Client();
                amazonS3.createBucket( bucketName );
                amazonS3.shutdown();
            } catch ( AmazonS3Exception e ) {
                result = e.getErrorCode();
            }
        }
    }

    class DropBucket {
        public String result = "";

        @ExecuteOrder(step = 1, desc = "删除")
        public void run() throws Exception {
            AmazonS3 amazonS3 = S3Utils.buildS3Client();
            try {
                amazonS3.deleteBucket( bucketName );
            } catch ( AmazonS3Exception e ) {
                result = e.getErrorCode();
            } finally {
                amazonS3.shutdown();
            }
        }
    }
}
