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
 * @Description SCM-3369:带prefix、delimiter、marker和maxkeys匹配查询对象元数据列表
 * @author fanyu
 * @Date 2019.06.20
 * @version 1.00
 */

public class ListObjects3369 extends TestScmBase {
    private String bucketName = "bucket3369";
    private String prefix = "dir1/";
    private String delimiter = "/";
    private String[] keyNames = { "a/test0_3369", "a/test1_3369",
            "dir1/atest2_3369.png", "dir1/dir2/aa/dd/test3_3369",
            "dir1/dir2/dir3/test/4_3369", "dir1/dir2/xx/test5_3369",
            "dir1/test6_3369", "dir1/test/7_3369", "dir1/test/8_3369",
            "dir1/test/aa9_3369", "fdir1/test10/_3369",
            "testdir1/11.txt_3369" };
    private AmazonS3 s3Client = null;
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        for ( int i = 0; i < keyNames.length; i++ ) {
            s3Client.putObject( bucketName, keyNames[ i ],
                    "testContest_" + keyNames[ i ] );
        }
    }

    @Test
    public void testListObjects() throws Exception {
        // test a: match nums < maxkeys
        int maxKeysA = 10;
        listObjectsAndCheckResult( prefix, delimiter, maxKeysA );

        // test b: match nums > maxkeys
        int maxKeysB = 2;
        listObjectsAndCheckResult( prefix, delimiter, maxKeysB );
        runSuccess1 = true;
    }

    @Test
    public void testMaxKeyZero() throws Exception {
        // test c: maxKey = 0
        int maxKey = 0;
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withDelimiter( delimiter ).withMaxKeys( maxKey );
        ObjectListing result = s3Client.listObjects( request );
        Assert.assertEquals( result.isTruncated(), false,
                "result.isTruncated() must be false" );
        Assert.assertEquals( result.getCommonPrefixes().size(), 0 );
        Assert.assertEquals( result.getObjectSummaries().size(), 0 );
        runSuccess2 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess1 && runSuccess2 ) {
                S3Utils.deleteAllObjectVersions( s3Client, bucketName );
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void listObjectsAndCheckResult( String prefix, String delimiter,
            int maxKeys ) {
        String marker = "dir1/dir2/aa/";
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withMarker( marker )
                .withPrefix( prefix ).withDelimiter( delimiter )
                .withMaxKeys( maxKeys );
        ObjectListing result;
        List< String > commonPrefixes = new ArrayList<>();
        List< String > queryKeyList = new ArrayList<>();
        do {
            result = s3Client.listObjects( request );
            List< String > oneGetCommPrefixes = result.getCommonPrefixes();
            commonPrefixes.addAll( oneGetCommPrefixes );
            List< S3ObjectSummary > objects = result.getObjectSummaries();
            List< String > oneQueryKeyList = new ArrayList<>();
            for ( S3ObjectSummary os : objects ) {
                String key = os.getKey();
                oneQueryKeyList.add( key );
                queryKeyList.add( key );
            }
            String nextMarker = result.getNextMarker();
            request.setMarker( nextMarker );

            int eachListNums = oneGetCommPrefixes.size()
                    + oneQueryKeyList.size();
            if ( eachListNums > maxKeys ) {
                Assert.fail( "list nums error! commonPrefixes: "
                        + oneGetCommPrefixes.toString() + "  contents:"
                        + oneQueryKeyList.toString() + "\n eachListNums="
                        + eachListNums + "  maxKeys=" + maxKeys );
            }
        } while ( result.isTruncated() );

        // check the commprefixList and contents
        List< String > expCommprefixLists = new ArrayList<>();
        expCommprefixLists.add( "dir1/test/" );

        List< String > expContentLists = new ArrayList<>();
        expContentLists.add( "dir1/test6_3369" );

        Collections.sort( expCommprefixLists );
        Assert.assertEquals( commonPrefixes, expCommprefixLists,
                "commonPrefixes:" + commonPrefixes.toString()
                        + "\n expCommprefixList:"
                        + expCommprefixLists.toString() );
        Collections.sort( expContentLists );
        Assert.assertEquals( queryKeyList, expContentLists,
                "matchContents:" + queryKeyList.toString()
                        + "\n expContentList:" + expContentLists.toString() );

    }
}
