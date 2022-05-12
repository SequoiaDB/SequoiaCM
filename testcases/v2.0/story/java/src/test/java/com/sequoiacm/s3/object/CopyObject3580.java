package com.sequoiacm.s3.object;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3580:指定ifUnModifiedSince和ifModifiedSince条件复制对象，
 *              源对象不匹配ifUnModifiedSince
 * @author fanyu
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3580 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3580";
    private String srcKeyName = "src/bb/object3580";
    private String destKeyName = "dest/object3580";
    private AmazonS3 s3Client = null;
    private long lastModifiedTime = 0;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName, "testcontent1" );
        s3Client.putObject( bucketName, srcKeyName, "testcontent2" );
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, srcKeyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        lastModifiedTime = lastModifiedDate.getTime();
    }

    @Test
    public void testCopyObject() throws Exception {
        // set date 2 minutes early than the last modified time
        long beforeTimestamp = lastModifiedTime - 2 * 60 * 1000l;
        Date beforeDate = new Date( beforeTimestamp );

        // set date 3 minutes early than last modified time
        long timestamp = lastModifiedTime - 3 * 60 * 1000l;
        Date noModifiedDate = new Date( timestamp );

        // copyObject
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.withModifiedSinceConstraint( beforeDate )
                .withUnmodifiedSinceConstraint( noModifiedDate );
        CopyObjectResult result = s3Client.copyObject( request );
        Assert.assertNull( result, "does not match object!" );
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, destKeyName ),
                "the destObject should be no exist!" );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }
}
