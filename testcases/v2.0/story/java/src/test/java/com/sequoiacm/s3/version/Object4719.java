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

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @descreption SCM-4719 :: 指定versionIdMarker != null查询对象版本列表，且版本列表中有versionId
 *              == null的记录
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4719 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4719";
    private String[] objectNames = { "4719:012", "4719:345", "4719:678",
            "4719:9AB", "4719:CDE" };
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
            s3Client.putObject( bucketName, objectName,
                    "" + UUID.randomUUID() );
        }
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.SUSPENDED );
        for ( String objectName : objectNames ) {
            s3Client.putObject( bucketName, objectName,
                    "" + UUID.randomUUID() );
        }
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            s3Client.putObject( bucketName, objectName,
                    "" + UUID.randomUUID() );
        }
    }

    // null的内部versionId小于versionIdMarker记录
    @Test
    private void test() {
        int index = 0;
        String keyMarker = objectNames[ index ];
        String versionIdMarker = ( versionNum + 1 ) + ".0";
        Integer maxResults = 7;

        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker )
                .withMaxResults( maxResults ) );

        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( int i = index; i < maxResults / versionNum; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                if ( j != 2 ) {
                    expMap.add( objectNames[ i ],  j + ".0" );
                } else {
                    expMap.add( objectNames[ i ], "null" );
                }
            }
        }
        expMap.add( objectNames[ 2 ], "3.0" );

        Assert.assertTrue( vsList.isTruncated(),
                "vsList.isTruncated() must be true" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );

        // null的内部versionId大于versionIdMarker记录
        Integer maxResults1 = 7;
        String nextKeyMarker = vsList.getNextKeyMarker();
        String nextVersionIdMarker = "2.0";

        VersionListing vsList1 = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withKeyMarker( nextKeyMarker )
                        .withVersionIdMarker( nextVersionIdMarker )
                        .withMaxResults( maxResults1 ) );

        // expected results
        MultiValueMap< String, String > expMap1 = new LinkedMultiValueMap< String, String >();
        expMap1.add( objectNames[ 2 ], "1.0" );
        for ( int i = 3; i < objectNames.length; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                if ( j != 2 ) {
                    expMap1.add( objectNames[ i ], j + ".0" );
                } else {
                    expMap1.add( objectNames[ i ], "null" );
                }
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
