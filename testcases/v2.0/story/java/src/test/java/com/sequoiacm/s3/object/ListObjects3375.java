package com.sequoiacm.s3.object;

import java.util.ArrayList;
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
 * @Description SCM-3375:带前缀prefix+delimiter查询对象元数据列表,不匹配prefix
 * @author wuyan
 * @Date 2019.06.20
 * @version 1.00
 */

public class ListObjects3375 extends TestScmBase {
    private String bucketName = "bucket3375";
    private String[] keyList = { "dir/atest/_3375.png",
            "dir/test/test/_3375.png", "dir1/dir2/test_3375.png",
            "dir1/dir2/test_3375", "dir1_3375" };
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
                    "testcontext18575_" + i );
        }
    }

    @Test
    private void testListObjects() {
        List< String > matchPrefixList = new ArrayList<>();
        List< String > matchContentsList = new ArrayList<>();

        String delimiter1 = "/";
        String prefix = "dir1?";
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName );
        request.withDelimiter( delimiter1 ).withPrefix( prefix );
        ObjectListing result = s3Client.listObjects( request );
        List< String > commonPrefixes = result.getCommonPrefixes();
        Assert.assertEquals( commonPrefixes, matchPrefixList,
                "actPrefixes:" + commonPrefixes.toString() + "\n expPrefixes:"
                        + matchPrefixList.toString() );

        List< String > actContentsList = new ArrayList<>();
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            actContentsList.add( key );
        }
        Assert.assertEquals( actContentsList, matchContentsList,
                "actContents:" + actContentsList.toString() + "\n expContents:"
                        + matchContentsList.toString() );
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
}
