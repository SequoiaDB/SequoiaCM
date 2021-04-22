package com.sequoiacm.s3.object;

import java.util.ArrayList;
import java.util.Collections;
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
 * @Description SCM-3365:带prefix和marker查询对象元数据列表
 * @author fanyu
 * @Date 2019.06.20
 * @version 1.00
 */
public class ListObjects3365 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3365";
    private String key = "object3365.png";
    private AmazonS3 s3Client = null;
    private int objectNums = 20;
    private String prefix = "dir_1/prefix/test3365";

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testListObjects() throws Exception {
        List< String > matchPrefixKeyList = putObjects();
        int startPosition = 5;
        listObjectsAndCheckResult( matchPrefixKeyList, startPosition );
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

    private void listObjectsAndCheckResult( List< String > expKeyList,
            int startPosition ) {
        Collections.sort( expKeyList );
        String marker = expKeyList.get( startPosition );
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withMarker( marker )
                .withPrefix( prefix );
        List< String > queryKeyList = new ArrayList<>();
        ObjectListing result = s3Client.listObjects( request );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            queryKeyList.add( key );
        }

        // check the keyName
        expKeyList.subList( 0, startPosition + 1 ).clear();
        Assert.assertEquals( queryKeyList, expKeyList,
                "queryKey:" + queryKeyList.toString() + "\n expKey:"
                        + expKeyList.toString() );
    }

    private List< String > putObjects() {
        List< String > matchPrefixKeyList = new ArrayList<>();
        String keyName;
        for ( int i = 0; i < objectNums; i++ ) {
            if ( i % 2 == 0 ) {
                // no match prefix
                keyName = key + "_" + i;
            } else {
                // match prefix
                keyName = prefix + "_" + i;
                matchPrefixKeyList.add( keyName );
            }
            s3Client.putObject( bucketName, keyName, "test3365" + i );
        }
        return matchPrefixKeyList;
    }
}
