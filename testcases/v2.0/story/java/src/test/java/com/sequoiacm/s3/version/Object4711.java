package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @descreption SCM-4711 :: 带delimiter和maxkeys查询对象版本列表，不匹配maxKeys
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4711 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4711";
    private String[] objectNames = { "abc%4711", "cde%4711", "fgh%4711",
            "ijk%4711", "lmn%4711" };
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
        String delimiter = "%";
        Integer maxResults = versionNum * objectNames.length + 1;
        // maxResults > versionNum*objectNames.length
        VersionListing vsList = s3Client.listVersions( new ListVersionsRequest()
                .withBucketName( bucketName ).withDelimiter( delimiter )
                .withMaxResults( maxResults ) );
        // expected results
        List< String > expCommonPrefixes = S3Utils.getCommPrefixes( objectNames,
                "", delimiter );
        // check results
        if ( !vsList.isTruncated() ) {
            S3Utils.checkListVSResults( vsList, expCommonPrefixes,
                    new LinkedMultiValueMap< String, String >() );
        } else {
            Assert.fail( "vsList.isTruncated() must be false" );
        }
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
