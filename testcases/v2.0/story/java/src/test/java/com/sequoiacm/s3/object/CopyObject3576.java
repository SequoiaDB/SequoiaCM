package com.sequoiacm.s3.object;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3576:指定ifMatch和ifNoneMatch条件复制对象，指定源对象不匹配ifMatch
 * @author wuyan
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3576 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3576";
    private String srcKeyName = "src/bb/object3576";
    private String destKeyName = "dest/object3576";
    private AmazonS3 s3Client = null;
    private String hisVersionContent0 = "hisVersion0";
    private String hisVersionContent1 = "hisVersionContent1";

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName, hisVersionContent0 );
        s3Client.putObject( bucketName, srcKeyName, hisVersionContent1 );
        s3Client.putObject( bucketName, srcKeyName, "curVersionContent0002" );
    }

    @Test
    public void testCopyObject() throws Exception {
        String hisVersionETag0 = TestTools
                .getMD5( hisVersionContent0.getBytes() );
        String hisVersionETag1 = TestTools
                .getMD5( hisVersionContent1.getBytes() );
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.withMatchingETagConstraint( hisVersionETag1 )
                .withNonmatchingETagConstraint( hisVersionETag0 );
        CopyObjectResult result = s3Client.copyObject( request );
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
