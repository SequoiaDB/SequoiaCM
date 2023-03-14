package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @descreption SCM-4250 :: 创建桶，指定ws(region)未关闭目录
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4250 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4250";
    private ScmWorkspace ws_test = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4250";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        int siteNum = ScmInfo.getSiteNum();

        ws_test = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws ScmException, IOException {
        try {
            ScmFactory.Bucket.createBucket( ws_test, bucketName );
            Assert.fail( "Create bucket：" + bucketName + " should failed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorDescription(),
                    "Unsupported operation" );
        }

        try {
            s3Client.createBucket(
                    new CreateBucketRequest( bucketName, wsName ) );
            Assert.fail( "Create bucket：" + bucketName + " should failed" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "CreateBucketFailed" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }
}
