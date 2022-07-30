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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @descreption SCM-4718 :: 指定encoding-type查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4718 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4718";
    private String[] objectNames = { "BEL", "4718!(4718 4718.txt).txt!",
            "4718!-/_!", "4718!.|*'!", "4718!#1", "4718!#2" };
    private AmazonS3 s3Client = null;
    private int versionNum = 2;

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
    private void test() throws Exception {
        String prefix = "4718!";
        String delimiter = "!";
        String encodingType = "url";
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withDelimiter( delimiter ).withEncodingType( encodingType ) );

        // expected results
        List< String > expCommonPrefixes = S3Utils
                .getCommPrefixes( objectNames, prefix, delimiter );
        List< String > expCommonPrefixesByEncode = new ArrayList< String >();
        for ( String expCommonPrefix : expCommonPrefixes ) {
            expCommonPrefixesByEncode
                    .add( URLEncoder.encode( expCommonPrefix, "utf-8" ) );
        }
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap< String, String >();
        for ( int i = 4; i < objectNames.length; i++ ) {
            for ( int j = versionNum ; j > 0; j-- ) {
                expMap.add( URLEncoder.encode( objectNames[ i ], "utf-8" ),
                        j + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false );
        S3Utils.checkListVSResults( vsList, expCommonPrefixesByEncode,
                expMap );
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
