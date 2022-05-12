package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
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
 * @Descreption SCM-3262:不同用户创建桶，部分桶名相同（标准模式）
 * @Author YiPan
 * @Date 2021/3/5
 */
public class CreateBucket3262 extends TestScmBase {
    private AmazonS3 s3A = null;
    private AmazonS3 s3B = null;
    private String bucketNameA = "bucket3262";
    private String bucketNameC = "bucket3262c";
    private String username = "user3262";
    private String password = "user3262123456";
    private ScmSession session = null;
    private String[] accessKeys = null;

    @BeforeClass
    private void setUp() throws Exception {
        // 默认用户连接
        s3A = S3Utils.buildS3Client();
        // 新建用户连接
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ScmAuthUtils.createAdminUserGrant( session, s3WorkSpaces, username,
                password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        ScmAuthUtils.checkPriorityByS3( accessKeys, s3WorkSpaces );
        s3B = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );

        S3Utils.clearBucket( s3A, bucketNameA );
        S3Utils.clearBucket( s3B, bucketNameC );
    }

    @Test
    public void test() throws ScmException {
        // 用户A创建桶A
        s3A.createBucket( bucketNameA );
        try {
            // 用户B创建桶A
            s3B.createBucket( bucketNameA );
            Assert.fail( "expect fail but success" );
        } catch ( AmazonS3Exception e ) {
            System.err.println( e.getMessage() );
            Assert.assertEquals( e.getErrorCode(), "BucketAlreadyExists" );
        }
        // 用户B创建桶C
        s3B.createBucket( bucketNameC );

        // 查看所有桶
        Assert.assertTrue( s3A.doesBucketExistV2( bucketNameA ) );
        Assert.assertTrue( s3A.doesBucketExistV2( bucketNameC ) );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            S3Utils.clearBucket( s3A, bucketNameA );
            S3Utils.clearBucket( s3B, bucketNameC );
            s3A.shutdown();
            s3B.shutdown();
        } finally {
            ScmFactory.User.deleteUser( session, username );
            session.close();
        }
    }
}
