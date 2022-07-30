package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4634 :: 指定versionId获取带deleteMarker标记的文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4634 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4634";
    private String objectName = "object4634";
    private AmazonS3 s3Client = null;
    private int versionNum = 3;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test
    private void test() throws Exception {
        try {
            s3Client.getObject(
                    new GetObjectRequest( bucketName, objectName, "1.0" ) );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }

        s3Client.deleteObject( bucketName, objectName );
        try {
            s3Client.getObject(
                    new GetObjectRequest( bucketName, objectName, "1.0" ) );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
        }

        // create multiple versions object in the bucket
        for ( int i = 0; i < versionNum; i++ ) {
            s3Client.putObject( bucketName, objectName, "content" );
        }

        try {
            s3Client.getObject(
                    new GetObjectRequest( bucketName, objectName, "1.0" ) );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
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
