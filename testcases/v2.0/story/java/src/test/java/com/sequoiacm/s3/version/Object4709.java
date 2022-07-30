package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @descreption SCM-4709 :: 带delimiter和maxkeys查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4709 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private String bucketName = "bucket4709";
    private String[] objectNames = { "air/4709", "dir/4709", "fire4709",
            "test4709" };
    private AmazonS3 s3Client = null;
    private int versionNum = 4;

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

    // 指定maxkeys一次返回所有匹配条件的对象
    @Test
    private void testGtMaxKeys() {
        String delimiter = "/";
        // maxResults > versionNum*objectNames.length
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withMaxResults( versionNum * objectNames.length + 1 ) );

        // expected results
        List< String > expCommonPrefixes = new ArrayList<>();
        expCommonPrefixes.add( "air/" );
        expCommonPrefixes.add( "dir/" );

        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = 2; i < objectNames.length; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames[ i ], j + ".0" );
            }
        }

        if ( !vsList.isTruncated() ) {
            S3Utils.checkListVSResults( vsList, expCommonPrefixes, expMap );
        } else {
            Assert.fail( "vsList.isTruncated() must be false" );
        }
        runSuccess1 = true;
    }

    // 指定maxkeys多次返回所有匹配条件的对象
    @Test
    private void testLtMaxKeys() throws Exception {
        String delimiter = "/";
        // maxResults < versionNum*objectNames.length
        VersionListing vsList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withDelimiter( delimiter ).withMaxResults( 1 ) );
        List< String > expCommonPrefixes1 = new ArrayList< String >();
        expCommonPrefixes1.add( "air/" );
        if ( vsList.isTruncated() ) {
            S3Utils.checkListVSResults( vsList, expCommonPrefixes1,
                    new LinkedMultiValueMap< String, String >() );
        } else {
            Assert.fail( "vsList.isTruncated() must be true" );
        }

        // maxResults > versionNum*objectNames.length
        String nextKeyMarker = vsList.getNextKeyMarker();
        String nestVersionIdMarker = vsList.getNextVersionIdMarker();
        VersionListing vsList1 = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withKeyMarker( nextKeyMarker )
                        .withVersionIdMarker( nestVersionIdMarker )
                        .withDelimiter( delimiter )
                        .withMaxResults( versionNum * objectNames.length ) );
        // expected results
        List< String > expCommonPrefixes2 = new ArrayList< String >();
        expCommonPrefixes2.add( "dir/" );
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = 2; i < objectNames.length; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames[ i ], j + ".0" );
            }
        }
        if ( !vsList1.isTruncated() ) {
            S3Utils.checkListVSResults( vsList1, expCommonPrefixes2, expMap );
        } else {
            Assert.fail( "vsList.isTruncated() must be false" );
        }
        runSuccess2 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess1 && runSuccess2 ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
