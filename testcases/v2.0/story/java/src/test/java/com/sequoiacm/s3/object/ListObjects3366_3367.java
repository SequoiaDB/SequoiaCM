package com.sequoiacm.s3.object;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3366:带prefix、delimiter和marker查询对象元数据列表，不匹配prefix
 *              SCM-3367:带prefix、delimiter和marker查询对象元数据列表，不匹配delimiter
 * @author fanyu
 * @Date 2019.06.20
 * @version 1.00
 */
public class ListObjects3366_3367 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3366";
    private String key = "object3366.png";
    private AmazonS3 s3Client = null;
    private int objectNums = 10;
    private String delimiter = "/";
    private List< String > matchKeyList = new ArrayList<>();

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        putObjects();
    }

    @Test
    public void testListObjects() {
        // test 3366: no match prefix
        String marker = "dir/";
        String prefixA = "test3366";
        listObjectsAndCheckResultA( marker, prefixA, delimiter );

        // test 18570: no match delimiter
        int startPosition = 1;
        String prefixB = "dir/";
        String delimiterB = "/a";
        listObjectsAndCheckResultB( startPosition, prefixB, delimiterB );
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

    // no mathch prefix
    private void listObjectsAndCheckResultA( String matchMarker,
            String matchPrefix, String matchDelimiter ) {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withMarker( matchMarker )
                .withPrefix( matchPrefix ).withDelimiter( matchDelimiter );
        ObjectListing result = s3Client.listObjects( request );
        List< String > actCommonPrefixes = result.getCommonPrefixes();
        // no match key,the commonprefixes is 0
        Assert.assertEquals( actCommonPrefixes.size(), 0 );

        List< String > queryKeyList = new ArrayList<>();
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            queryKeyList.add( key );
        }

        Assert.assertEquals( queryKeyList.size(), 0,
                "queryKey=" + queryKeyList.toString() );
    }

    // no match delimiter
    private void listObjectsAndCheckResultB( int startPosition,
            String matchPrefix, String matchDelimiter ) {
        String matchMarker = matchKeyList.get( startPosition );
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withMarker( matchMarker )
                .withPrefix( matchPrefix ).withDelimiter( matchDelimiter );
        try {
            s3Client.listObjects( request );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( AmazonS3Exception e ) {
            if ( e.getStatusCode() != 400 ) {
                throw e;
            }
        }
    }

    private void putObjects() {
        String keyName;
        for ( int i = 0; i < objectNums; i++ ) {
            if ( i % 2 == 0 ) {
                keyName = key + "_" + i;
            } else {
                String prefix = "dir/" + i;
                // the key include prefix and delimiter
                keyName = prefix + "_" + delimiter + "_" + key;
                matchKeyList.add( keyName );
            }
            s3Client.putObject( bucketName, keyName, "test3366" + i );
        }
    }
}
