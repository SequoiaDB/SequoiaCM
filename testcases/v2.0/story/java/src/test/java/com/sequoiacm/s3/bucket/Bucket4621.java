package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4621 :: 不同用户设置/获取桶版本控制状态
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4621 extends TestScmBase {
    private AmazonS3 s3A = null;
    private AmazonS3 s3B = null;
    private boolean runSuccess = false;
    private final String bucketName = "bucket4621";
    private ScmSession session;
    private String userName = "user4621";
    private String passWord = "passwd4621";
    private String[] accessKeys = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3A = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3A, bucketName );

        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ScmAuthUtils.createUser( session, userName, passWord );

        accessKeys = ScmAuthUtils.refreshAccessKey( session, userName, passWord,
                null );
        s3B = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );
    }

    @Test
    public void test() throws Exception {
        s3A.createBucket( bucketName );

        try {
            S3Utils.setBucketVersioning( s3B, bucketName, "Enabled" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 403 );
            Assert.assertEquals( e.getErrorCode(), "AccessDenied" );
        }

        try {
            S3Utils.setBucketVersioning( s3B, bucketName, "Suspended" );
            Assert.fail("set bucket versioning configuration should be failed");
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 403 );
            Assert.assertEquals( e.getErrorCode(), "AccessDenied" );
        }

        try {
            s3B.getBucketVersioningConfiguration( bucketName );
            Assert.fail("get bucket versioning configuration should be failed");
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 403 );
            Assert.assertEquals( e.getErrorCode(), "AccessDenied" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3A, bucketName );
            }
        } finally {
            if ( s3A != null ) {
                s3A.shutdown();
            }
            if ( s3B != null ) {
                s3B.shutdown();
            }
        }
    }
}
