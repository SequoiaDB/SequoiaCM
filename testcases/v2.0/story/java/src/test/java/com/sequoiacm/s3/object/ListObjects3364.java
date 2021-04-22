package com.sequoiacm.s3.object;

import java.io.IOException;
import java.util.ArrayList;
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
 * @Description SCM-3364:带prefix和marker查询对象元数据列表，匹配部分条件
 * @author fanyu
 * @Date 2019.06.17
 * @version 1.00
 */
public class ListObjects3364 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3364";
    private String key = "aa/bb/object3364.png";
    private AmazonS3 s3Client = null;
    private int objectNums = 10;

    @BeforeClass
    private void setUp() throws IOException {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testListObjects() throws Exception {
        List< String > keyList = putObjects();
        // test a: match marker not match prefix
        int startPositionA = 0;
        String markerA = keyList.get( startPositionA );
        String prefixA = "pre3364";
        listObjectsAndCheckResult( markerA, prefixA );

        // test b: match prefix not match marker
        int startPositionB = objectNums - 1;
        String markerB = keyList.get( startPositionB );
        String prefixB = "aa/bb/object3364";
        listObjectsAndCheckResult( markerB, prefixB );
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

    private void listObjectsAndCheckResult( String marker, String prefix ) {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withMarker( marker );
        ObjectListing result = s3Client.listObjects( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        List< String > commonPrefixes = result.getCommonPrefixes();
        // misMatchObject, the list size is 0
        Assert.assertEquals( objects.size(), 0 );
        Assert.assertEquals( commonPrefixes.size(), 0 );
    }

    private List< String > putObjects() {
        List< String > keyList = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            String keyName = key + "_" + i;
            s3Client.putObject( bucketName, keyName, "test3364" + i );
            keyList.add( keyName );
        }
        return keyList;
    }
}
