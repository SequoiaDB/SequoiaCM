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
 * @descreption SCM-4717 :: 多次查询结果在commprefix中有相同记录
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4717 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4717";
    private String[] objectNames = { "/aa/bb/test1", "/aa/bb/test2",
            "/bb/cc/test1", "/bb/cc/test2", "/cc/dd/test1", "/cc/dd/test2" };
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
    private void test1() throws Exception {
        String prefix = "/";
        String delimiter = "/";
        Integer maxResults = 1;

        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withDelimiter( delimiter ).withMaxResults( maxResults ) );
        // expected results
        List< String > expCommonPrefixes = new ArrayList< String >();
        expCommonPrefixes.add( "/aa/" );

        // check
        Assert.assertEquals( vsList.isTruncated(), true,
                "vsList.isTruncated() must be true" );
        S3Utils.checkListVSResults( vsList, expCommonPrefixes,
                new LinkedMultiValueMap< String, String >() );

        // test isTruncated
        String nextKeyMarker = vsList.getNextKeyMarker();
        String nextVersionIdMarker = vsList.getNextVersionIdMarker();
        Integer maxResults2 = 2;
        VersionListing vsList2 = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withKeyMarker( nextKeyMarker )
                        .withVersionIdMarker( nextVersionIdMarker )
                        .withPrefix( prefix ).withDelimiter( delimiter )
                        .withMaxResults( maxResults2 ) );

        // expected results
        List< String > expCommonPrefixes2 = new ArrayList< String >();
        expCommonPrefixes2.add( "/bb/" );
        expCommonPrefixes2.add( "/cc/" );
        Assert.assertEquals( vsList2.isTruncated(), false,
                "vsList3.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList2, expCommonPrefixes2,
                new LinkedMultiValueMap< String, String >() );

        // add new object
        String newObjectName = "/dd";
        for ( int j = 0; j < versionNum; j++ ) {
            s3Client.putObject( bucketName, newObjectName,
                    "" + UUID.randomUUID() );
        }

        // test isTruncated
        Integer maxResults3 = 100;
        VersionListing vsList3 = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName )
                        .withKeyMarker( nextKeyMarker )
                        .withVersionIdMarker( nextVersionIdMarker )
                        .withPrefix( prefix ).withDelimiter( delimiter )
                        .withMaxResults( maxResults3 ) );

        // expected results
        List< String > expCommonPrefixes3 = new ArrayList< String >();
        expCommonPrefixes3.add( "/bb/" );
        expCommonPrefixes3.add( "/cc/" );
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int k = versionNum; k > 0; k-- ) {
            expMap.add( newObjectName, k + ".0" );
        }
        Assert.assertEquals( vsList3.isTruncated(), false,
                "vsList3.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList3, expCommonPrefixes3, expMap );
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
