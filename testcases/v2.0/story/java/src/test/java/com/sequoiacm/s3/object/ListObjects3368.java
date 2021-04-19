package com.sequoiacm.s3.object;

import java.io.IOException;
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
 * @Description SCM-3368: To get a list by listObjectV1.specify matching
 *              delimiter and maxkeys. test a: maxkeys is less than the matching
 *              records test b: maxkeys is greater than the matching records
 * @author wuyan
 * @Date 2019.06.20
 * @version 1.00
 */
public class ListObjects3368 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3368";
    private String keyName = "object3368";
    private AmazonS3 s3Client = null;
    private int keyNums = 10;
    private String delimiter = "/";
    private List< String > keyList = new ArrayList<>();
    private List< String > noMatchDelimiterKeyList = new ArrayList<>();
    private List< String > matchDelimiterKeyList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws IOException {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        putObjects( keyNums );
    }

    @Test
    public void testListObjects() throws Exception {
        // test a: match nums > maxkeys
        int maxKeysA = 3;
        listObjectsAndCheckResult( matchDelimiterKeyList,
                noMatchDelimiterKeyList, maxKeysA );

        // test b: match nums < maxkeys
        int maxKeysB = 51;
        listObjectsAndCheckResult( matchDelimiterKeyList,
                noMatchDelimiterKeyList, maxKeysB );

        // test c: match nums = maxkeys
        int maxKeysC = keyNums;
        listObjectsAndCheckResult( matchDelimiterKeyList,
                noMatchDelimiterKeyList, maxKeysC );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for ( String keyName : keyList ) {
                    s3Client.deleteObject( bucketName, keyName );
                }
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void listObjectsAndCheckResult( List< String > expCommprefixLists,
            List< String > expContentLists, int maxKeys ) throws IOException {
        List< String > matchContentList = new ArrayList<>();
        List< String > matchDelimiterList = new ArrayList<>();
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withMaxKeys( maxKeys );
        ObjectListing result;

        do {
            result = s3Client.listObjects( request );
            List< String > commonPrefixes = result.getCommonPrefixes();
            matchDelimiterList.addAll( commonPrefixes );

            // objects do not match delimiter are displayed in contents,num is
            // less than Maxkeys
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            List< String > queryKeyList = new ArrayList<>();
            for ( S3ObjectSummary os : objects ) {
                String key = os.getKey();
                queryKeyList.add( key );
            }
            matchContentList.addAll( queryKeyList );

            String nextMarker = result.getNextMarker();
            request.setMarker( nextMarker );

            int eachListNums = commonPrefixes.size() + queryKeyList.size();
            if ( eachListNums > maxKeys ) {
                Assert.fail( "list nums error! commonPrefixes: "
                        + commonPrefixes.toString() + "  contents:"
                        + queryKeyList.toString() );
            }
        } while ( result.isTruncated() );

        // check the commprefixList and contents
        Assert.assertEquals( matchDelimiterList, expCommprefixLists,
                "matchDelimiterList:" + matchDelimiterList.toString()
                        + "\n commprefixList:"
                        + expCommprefixLists.toString() );
        Assert.assertEquals( matchContentList, expContentLists,
                "matchContents:" + matchContentList.toString()
                        + "\n contentList:" + expContentLists.toString() );

    }

    private void putObjects( int objectNums ) {
        String subKeyName;
        for ( int i = 0; i < objectNums; i++ ) {
            if ( i % 2 == 0 ) {
                subKeyName = keyName + "_" + i + "/" + "aatest.png";
                matchDelimiterKeyList.add( keyName + "_" + i + "/" );
            } else {
                subKeyName = keyName + "_" + i;
                noMatchDelimiterKeyList.add( subKeyName );
            }
            s3Client.putObject( bucketName, subKeyName, "content_3368_" + i );
            keyList.add( subKeyName );
            Collections.sort( matchDelimiterKeyList );
            Collections.sort( noMatchDelimiterKeyList );
        }
    }
}
