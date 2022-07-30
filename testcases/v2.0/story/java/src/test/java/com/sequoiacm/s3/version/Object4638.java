package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @descreption SCM-4638 :: 指定vesion获取对象，其中指定key不存在
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4638 extends TestScmBase {
    private boolean runSuccess = false;
    private String keya = "aa/bb/object4638a";
    private String keyb = "aa/bb/object4638b";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteObjectAllVersions( s3Client,
                TestScmBase.enableVerBucketName, keya );
    }

    @Test
    public void testGetObject() throws Exception {
        // Do not specify versionId
        try {
            s3Client.getObject( enableVerBucketName, keya );
            Assert.fail( "get not exist key must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchKey" );
        }

        // specify versionId
        try {
            GetObjectRequest request = new GetObjectRequest(
                    enableVerBucketName, keyb, "1.0" );
            s3Client.getObject( request );
            Assert.fail( "specify versionId get not exist key must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( s3Client != null ) {
            s3Client.shutdown();
        }
    }
}
