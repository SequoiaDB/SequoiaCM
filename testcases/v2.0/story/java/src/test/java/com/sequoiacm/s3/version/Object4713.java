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
 * @descreption SCM-4713 :: 带keyMarker、versionIdMarker查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4713 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4713";
    private String[] objectNames = { "4713%abc", "4713%bcd", "4713%cde",
            "4713%def", "4713%efg" };
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

    @Test
    private void test() {
        int index = 3;
        String keyMarker = objectNames[ index ];
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( keyMarker )
                .withVersionIdMarker( versionNum + ".0" ) );

        // expected results
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( int i = versionNum - 1; i > 0; i-- ) {
            expMap.add( objectNames[ index ], i + ".0" );
        }
        for ( int i = index + 1; i < objectNames.length; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames[ i ], j + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(), expMap );
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
