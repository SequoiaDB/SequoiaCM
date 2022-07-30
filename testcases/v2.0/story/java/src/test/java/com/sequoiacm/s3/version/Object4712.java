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

import java.util.ArrayList;
import java.util.UUID;

/**
 * @descreption SCM-4712 :: 带keyMarker、versionIdMarker和maxkeys查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4712 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private boolean runSuccess4 = false;
    private String bucketName = "bucket4712";
    private String[] objectNames = { "123#4712", "234#4712", "345#4712",
            "456#4712", "567#4712" };
    private AmazonS3 s3Client = null;
    private int versionNum = 10;

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

    // keyMarker、versionIdMarker匹配从第一条记录开始，设置maxkeys小于对象数
    @Test
    private void testHead() throws Exception {
        String keyMarker = objectNames[ 0 ];
        String versionIdMarker = ( versionNum + 1 ) + ".0";
        Integer maxResults = versionNum * ( objectNames.length - 1 );
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker )
                .withMaxResults( maxResults ) );

        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( int i = 0; i < objectNames.length - 1; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames[ i ], j + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), true,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
        runSuccess1 = true;
    }

    // keyMarker、versionIdMarker匹配最后1条记录，设置maxkeys大于1
    @Test
    private void testTail1() throws Exception {
        String keyMarker = objectNames[ objectNames.length - 1 ];
        String versionIdMarker = "2.0";
        Integer maxResults = 2;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker )
                .withMaxResults( maxResults ) );
        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        expMap.add( objectNames[ objectNames.length - 1 ], "1.0" );
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
        runSuccess2 = true;
    }

    // keyMarker、versionIdMarker指定最后1条对象key，指定maxkeys为1
    @Test
    private void testTail2() throws Exception {
        String keyMarker = objectNames[ objectNames.length - 1 ];
        String versionIdMarker = "1.0";
        Integer maxResults = 1;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker )
                .withMaxResults( maxResults ) );
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(),
                new LinkedMultiValueMap< String, String >() );
        runSuccess3 = true;
    }

    // keyMarker、versionIdMarker指定从第1条对象key开始，指定maxkeys为0
    @Test
    private void testMaxKeyZero() throws Exception {
        String keyMarker = objectNames[ 0 ];
        String versionIdMarker = "1.0";
        Integer maxResults = 0;
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionIdMarker )
                .withMaxResults( maxResults ) );
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(),
                new LinkedMultiValueMap< String, String >() );
        runSuccess4 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4 ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
