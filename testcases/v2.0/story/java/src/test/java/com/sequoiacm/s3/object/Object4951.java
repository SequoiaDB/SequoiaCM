package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;

/**
 * @descreption SCM-4951:S3接口列取文件，校验列取上限(maxKeys)
 * @author YiPan
 * @date 2022/7/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4951 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket4951";
    private String objectKey = "object4951";
    private String content = "test";
    private int maxKeys = 1000;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        BucketVersioningConfiguration config = new BucketVersioningConfiguration()
                .withStatus( BucketVersioningConfiguration.ENABLED );
        s3Client.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest( bucketName,
                        config ) );
    }

    @Test
    public void test() throws ScmException, IOException {
        // 上传对象
        putObjects( s3Client, bucketName );

        // listObjectV2
        ListObjectsV2Result listObjectsV2Result = s3Client
                .listObjectsV2( bucketName );
        List< S3ObjectSummary > objectSummariesV2 = listObjectsV2Result
                .getObjectSummaries();
        Assert.assertEquals( listObjectsV2Result.getMaxKeys(), maxKeys );
        Assert.assertEquals( objectSummariesV2.size(), maxKeys );

        // listObjectV2Request().withMaxKeys(1001)
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
                .withBucketName( bucketName ).withMaxKeys( maxKeys + 1 );
        listObjectsV2Result = s3Client.listObjectsV2( listObjectsV2Request );
        objectSummariesV2 = listObjectsV2Result.getObjectSummaries();
        Assert.assertEquals( listObjectsV2Result.getMaxKeys(), maxKeys + 1 );
        Assert.assertEquals( objectSummariesV2.size(), maxKeys );

        // listObject
        ObjectListing objectListing = s3Client.listObjects( bucketName );
        List< S3ObjectSummary > objectSummaries = objectListing
                .getObjectSummaries();
        Assert.assertEquals( objectListing.getMaxKeys(), maxKeys );
        Assert.assertEquals( objectSummaries.size(), maxKeys );

        // listObjectRequest().withMaxKeys(1001)
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName( bucketName ).withMaxKeys( maxKeys + 1 );
        objectListing = s3Client.listObjects( listObjectsRequest );
        objectSummaries = objectListing.getObjectSummaries();
        Assert.assertEquals( objectListing.getMaxKeys(), maxKeys + 1 );
        Assert.assertEquals( objectSummaries.size(), maxKeys );

        // listVersions
        VersionListing versionListing = s3Client.listVersions( bucketName,
                objectKey );
        List< S3VersionSummary > versionSummaries = versionListing
                .getVersionSummaries();
        Assert.assertEquals( versionListing.getMaxKeys(), maxKeys );
        Assert.assertEquals( versionSummaries.size(), maxKeys );

        // listVersionsRequest().withMaxKeys(1001)
        ListVersionsRequest listVersionsRequest = new ListVersionsRequest()
                .withBucketName( bucketName ).withMaxResults( maxKeys + 1 );
        versionListing = s3Client.listVersions( listVersionsRequest );
        versionSummaries = versionListing.getVersionSummaries();
        Assert.assertEquals( versionListing.getMaxKeys(), maxKeys + 1 );
        Assert.assertEquals( versionSummaries.size(), maxKeys );
        runSuccess = true;
    }

    private void putObjects( AmazonS3 s3Client, String bucketName ) {
        for ( int i = 0; i < maxKeys + 1; i++ ) {
            s3Client.putObject( bucketName, objectKey + i, content );
        }
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
