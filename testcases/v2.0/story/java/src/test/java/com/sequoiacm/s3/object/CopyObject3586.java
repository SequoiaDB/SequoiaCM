package com.sequoiacm.s3.object;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3586:指定ifNoneMatch和ifModifiedSince条件获取对象，不匹配ifNoneMatch
 * @author fanyu
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3586 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3586";
    private String srcKeyName = "src/bb/object3586";
    private String destKeyName = "dest/object3586";
    private String srcKeyContent = "testsrcObject!3586";
    private AmazonS3 s3Client = null;
    private long lastModifiedTime = 0;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName, srcKeyContent );
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, srcKeyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        lastModifiedTime = lastModifiedDate.getTime();
    }

    @Test
    public void testCopyObject() throws Exception {
        // set date 3 minutes early at the lastModified time
        long timestamp = lastModifiedTime - 3 * 60 * 1000l;
        Date date = new Date( timestamp );

        // copy object
        String etag = TestTools.getMD5( srcKeyContent.getBytes() );
        try {
            CopyObjectRequest request = new CopyObjectRequest( bucketName,
                    srcKeyName, bucketName, destKeyName );
            request.withModifiedSinceConstraint( date )
                    .withNonmatchingETagConstraint( etag );
            s3Client.copyObject( request );
            Assert.fail( "copyObject must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 304,
                    e.getErrorCode() + e.getErrorMessage() + "\netag:" + etag );
        }

        // check the result
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
