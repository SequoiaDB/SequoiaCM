package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @descreption SCM-4716 :: 指定nextVersionIdMarker匹配记录被删除，查询版本列表信息
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4716 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4716";
    private String[] objectNames = { "4716%123", "4716%456", "4716%789",
            "4716%ABC", "4716%DEF" };
    private AmazonS3 s3Client = null;
    private int versionNum = 3;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            for ( int j = 0; j < versionNum; j++ ) {
                s3Client.putObject( bucketName, objectName,
                        "" + UUID.randomUUID() );
            }
        }
    }

    @Test(groups = { GroupTags.base }) // bug:3986
    private void test() throws Exception {
        int index = 0;
        String keyMarker = objectNames[ index ];
        String versionIdMarker = (versionNum+1) +".0";
        Integer maxResults = 7;

        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker(  versionIdMarker  )
                .withMaxResults( maxResults ) );

        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = index; i < maxResults / versionNum; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames[ i ], j + ".0" );
            }
        }
        expMap.add( objectNames[ 2 ], "3.0" );

        Assert.assertTrue( vsList.isTruncated(),
                "vsList.isTruncated() must be true" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(),
                expMap );

        String nextKeyMarker = vsList.getNextKeyMarker();
        String nextVersionIdMarker = "2.0";
        s3Client.deleteVersion( bucketName, nextKeyMarker,
                nextVersionIdMarker );

        VersionListing vsList1 = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withKeyMarker( nextKeyMarker )
                        .withVersionIdMarker(
                                 nextVersionIdMarker )
                        .withMaxResults( maxResults ) );

        // expected results
        MultiValueMap< String, String > expMap1 = new LinkedMultiValueMap< String, String >();
        expMap1.add( objectNames[ 2 ], "1.0" );
        for ( int i = 3; i < objectNames.length; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap1.add( objectNames[ i ], j + ".0" );
            }
        }
        Assert.assertFalse( vsList1.isTruncated(),
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList1, new ArrayList< String >(),
                expMap1 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
