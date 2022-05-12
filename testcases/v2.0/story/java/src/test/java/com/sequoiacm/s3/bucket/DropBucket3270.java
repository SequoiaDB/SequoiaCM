package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.*;
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
 * @Descreption SCM-3270:删除其他用户的桶（标准模式）
 * @Author YiPan
 * @Date 2021/3/6
 */
public class DropBucket3270 extends TestScmBase {
    private AmazonS3 s3A = null;
    private AmazonS3 s3B = null;
    private String bucketName = "bucket3270";
    private String username = "user3270";
    private String password = "user3270123456";
    private ScmSession session = null;
    private String[] accessKeys = null;

    @BeforeClass
    private void setUp() throws Exception {
        // 默认用户连接
        s3A = S3Utils.buildS3Client();
        // 新建用户连接
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ScmAuthUtils.createUser( session, username, password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        s3B = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );

        S3Utils.clearBucket( s3A, bucketName );
    }

    //因问题单SEQUOIACM-842暂时屏蔽
    @Test(enabled = false)
    public void test() throws ScmException, InterruptedException {
        // 用户A创建桶A
        s3A.createBucket( bucketName );

        // 用户B删除A的桶
        try {
            s3B.deleteBucket( bucketName );
            Assert.fail( "expect fail but success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "AccessDenied" );
        }
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            S3Utils.clearBucket( s3A, bucketName );
            s3A.shutdown();
            s3B.shutdown();
        } finally {
            ScmFactory.User.deleteUser( session, username );
            session.close();
        }
    }
}
