package com.sequoiacm.s3.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3370:带prefix、marker、delimiter在设置nextMarker前后匹配条件不一致
 * @author fanyu
 * @Date 2019.06.20
 * @version 1.00
 */

public class ListObjects3370 extends TestScmBase {
    private String bucketName = "bucket3370";
    private String[] keyList = { "dir/atest/_3370.png",
            "dir1/dir2/test1_3370.png", "dir1/dir2/dir3/test2_3370",
            "dir1/a/test3_3370", "dir1/dir2/aa/test4_3370",
            "dir1/dir3/aa/test4_3370", "dir1/dir4/aa/test4_3370",
            "dir1/dir2/aa/cc/test5_3370", "dir1/dir2/aa/dd/test6_3370",
            "dir1/a3370", "dir3/a3370", "dir4a3370" };
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        for ( int i = 0; i < keyList.length; i++ ) {
            String subKeyName = keyList[ i ];
            s3Client.putObject( bucketName, subKeyName,
                    "testcontext3370_" + i );
        }
    }

    @Test
    private void testListObjects() {
        listObjectsAndCheckResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for ( int i = 0; i < keyList.length; i++ ) {
                    String keyName = keyList[ i ];
                    s3Client.deleteObject( bucketName, keyName );
                }
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void listObjectsAndCheckResult() {
        String delimiter1 = "/";
        String prefix = "dir1/";
        String marker = "dir";
        int maxKeys = 4;
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName );
        request.withDelimiter( delimiter1 ).withPrefix( prefix )
                .withMarker( marker ).withMaxKeys( maxKeys );
        ObjectListing result = s3Client.listObjects( request );
        List< String > commonPrefixes = result.getCommonPrefixes();
        List< String > matchPrefixList1 = new ArrayList<>();
        matchPrefixList1.add( "dir1/a/" );
        matchPrefixList1.add( "dir1/dir2/" );
        matchPrefixList1.add( "dir1/dir3/" );
        Collections.sort( matchPrefixList1 );
        Assert.assertEquals( commonPrefixes, matchPrefixList1,
                "actPrefixes:" + commonPrefixes.toString() + "\n expPrefixes:"
                        + matchPrefixList1.toString() );

        List< String > actContentsList1 = new ArrayList<>();
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            actContentsList1.add( key );
        }
        // check the key of contents
        List< String > matchContentsList1 = new ArrayList<>();
        matchContentsList1.add( "dir1/a3370" );
        Assert.assertEquals( actContentsList1, matchContentsList1 );
        Assert.assertTrue( result.isTruncated() );

        // second list
        String nextMarker = result.getNextMarker();
        request.setMarker( nextMarker );
        String delimiter2 = "/";
        String prefix2 = "dir";
        int maxKeys2 = 5;
        request.withDelimiter( delimiter2 ).withPrefix( prefix2 )
                .withMaxKeys( maxKeys2 );
        ObjectListing result2 = s3Client.listObjects( request );
        List< String > commonPrefixes2 = result2.getCommonPrefixes();
        List< String > matchPrefixList2 = new ArrayList<>();
        matchPrefixList2.add( "dir3/" );
        Assert.assertEquals( commonPrefixes2, matchPrefixList2,
                "actPrefixes:" + commonPrefixes2.toString() + "\n expPrefixes2:"
                        + matchPrefixList2.toString() );

        List< String > actContentsList2 = new ArrayList<>();
        List< S3ObjectSummary > objects2 = result2.getObjectSummaries();
        for ( S3ObjectSummary os2 : objects2 ) {
            String key = os2.getKey();
            actContentsList2.add( key );
        }
        Assert.assertFalse( result2.isTruncated() );

        // check the key of contents
        List< String > matchContentsList2 = new ArrayList<>();
        matchContentsList2.add( "dir4a3370" );
        Assert.assertEquals( actContentsList2, matchContentsList2 );
    }
}
