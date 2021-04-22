package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-3265:不存在桶，获取桶列表信息（标准模式）
 * @Author YiPan
 * @Date 2021/3/5
 */
public class ListBucket3265 extends TestScmBase {
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        List< Bucket > existbuckets = s3Client.listBuckets();
        for ( Bucket s : existbuckets ) {
            S3Utils.clearBucket( s3Client, s.getName() );
        }
    }

    @Test
    public void test() {
        List< String > expbuckets = new ArrayList<>();
        List< String > actbuckets = new ArrayList<>();
        List< Bucket > buckets = s3Client.listBuckets();
        for ( Bucket s : buckets ) {
            actbuckets.add( s.getName() );
        }
        Assert.assertEquals( actbuckets, expbuckets );
    }

    @AfterClass
    private void tearDown() {
        s3Client.shutdown();
    }
}
