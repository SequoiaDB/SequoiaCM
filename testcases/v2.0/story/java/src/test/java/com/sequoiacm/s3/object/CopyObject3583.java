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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3580:指定ifMatch和ifUnModifiedSince条件复制对象，源对象不匹配ifMatch
 * @author fanyu
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3583 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3583";
    private String srcKeyName = "src/bb/object3583";
    private String destKeyName = "dest/object3583";
    private String otherKeyName = "bb/object3583";
    private AmazonS3 s3Client = null;
    private String otherKeyContent = "otherKeyContent3583!";
    private long lastModifiedTime = 0;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName, "curVersionContent" );
        s3Client.putObject( bucketName, otherKeyName, otherKeyContent );
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, srcKeyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        lastModifiedTime = lastModifiedDate.getTime();
    }

    @Test
    public void testCopyObject() throws Exception {
        // set date 2 minutes later than lastModified time
        long timestamp = lastModifiedTime + 2 * 60 * 1000l;
        Date date = new Date( timestamp );

        // copy object
        String etag = TestTools.getMD5( otherKeyContent.getBytes() );
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.withUnmodifiedSinceConstraint( date )
                .withMatchingETagConstraint( etag );
        CopyObjectResult result = s3Client.copyObject( request );

        // check the result
        Assert.assertNull( result, "does not match object!" );
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, destKeyName ),
                "destObject no exist!" );

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
