package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4310:删除桶后再查询桶，验证缓存被及时清除
 * @Author YiPan
 * @CreateDate 2022/5/20
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4310 extends TestScmBase {
    private final String bucketName = "bucket4310";
    private ScmSession session;
    private AmazonS3 s3Client;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketName ) );

        // 删除桶
        s3Client.deleteBucket( bucketName );
        Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );

        try {
            ScmFactory.Bucket.getBucket( session, bucketName );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }
}