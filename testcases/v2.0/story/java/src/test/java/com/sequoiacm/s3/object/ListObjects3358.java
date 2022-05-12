package com.sequoiacm.s3.object;

import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3358:带前缀prefix查询对象元数据列表，匹配不到对象数据
 * @author fanyu
 * @Date 2018.11.19
 * @version 1.00
 */
public class ListObjects3358 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3358";
    private String key = "aa/bb/object3358";
    private AmazonS3 s3Client = null;
    private int matchObjectNums = 0;
    private String prefix = "/dir_1/prefix/test3358";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testCreateObject() throws Exception {
        putObjects();
        listObjectV1AndCheckResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void listObjectV1AndCheckResult() throws IOException {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName );
        request.withPrefix( prefix );
        ObjectListing result = s3Client.listObjects( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        Assert.assertEquals( objects.size(), matchObjectNums );
    }

    private void putObjects() {
        int objectNums = 10;
        for ( int i = 0; i < objectNums; i++ ) {
            String keyName = key + "_" + i;
            s3Client.putObject( bucketName, keyName, "testContext" );
        }
    }
}
