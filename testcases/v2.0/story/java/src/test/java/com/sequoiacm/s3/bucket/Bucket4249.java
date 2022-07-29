package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @descreption SCM-4249 :: 使用SCM API创建桶，指定无效桶名
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4249 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsNameA = "ws_4249_A";
    private String wsNameB = "ws_4249_B";
    private ScmWorkspace ws_test_A = null;
    private ScmWorkspace ws_test_B = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4249";
    private List< String > bucketNameList = new ArrayList<>();
    private List< String > IllegalBucketNames = Arrays.asList( "/", "\\", "%", ";",
            ":", "*", "?", "<", ">", "|" );

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsNameA, session );
        ScmWorkspaceUtil.deleteWs( wsNameB, session );

        ws_test_A = ScmWorkspaceUtil.createS3WS( session, wsNameA );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameA );

        ws_test_B = ScmWorkspaceUtil.createS3WS( session, wsNameB );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameB );
    }

    @Test
    public void test() throws ScmException, IOException {
        try {
            ScmFactory.Bucket.createBucket( null, bucketName );
            Assert.fail(
                    "Create bucket: " + bucketName + " should be failed" );
        } catch ( ScmException e ) {
            System.out.println( "Create bucket: " + bucketName + ", error:"
                    + e.getError().getErrorDescription() );
        }

        bucketNameList.add( "" );
        for ( String illegalChar : IllegalBucketNames ) {
            bucketNameList.add( bucketName + illegalChar );
            bucketNameList.add( bucketName + illegalChar + bucketName );
            bucketNameList.add( illegalChar + bucketName );
        }

        for ( String illegalName : bucketNameList ) {
            try {
                ScmFactory.Bucket.createBucket( ws_test_A, illegalName );
                Assert.fail(
                        "Create bucket: " + illegalName + " should be failed" );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError().getErrorType(), "INVALID_ARGUMENT");
            }
        }

        ScmFactory.Bucket.createBucket( ws_test_A, bucketName );
        try {
            ScmFactory.Bucket.createBucket( ws_test_A, bucketName );
            Assert.fail( "Create bucket：" + bucketName + " should failed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( "Bucket already exists",
                    e.getError().getErrorDescription() );
        }
        try {
            ScmFactory.Bucket.createBucket( ws_test_B, bucketName );
            Assert.fail( "Create bucket：" + bucketName + " should failed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( "Bucket already exists",
                    e.getError().getErrorDescription() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
                ScmWorkspaceUtil.deleteWs( wsNameA, session );
                ScmWorkspaceUtil.deleteWs( wsNameB, session );
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
