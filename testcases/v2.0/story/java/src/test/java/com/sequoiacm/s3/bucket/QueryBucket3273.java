package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3273:headBucket请求查询桶（标准模式）
 * @Author YiPan
 * @Date 2021/3/8
 */
public class QueryBucket3273 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3273";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        HeadBucketRequest request = new HeadBucketRequest( bucketName );
        // 创建查询
        s3Client.createBucket( bucketName );
        HeadBucketResult headBucketResult = s3Client.headBucket( request );
        checkWs( headBucketResult.getBucketRegion() );

        // 删除桶再次查询
        s3Client.deleteBucket( bucketName );
        try {
            s3Client.headBucket( request );
            Assert.fail( "expect fail but success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "404 Not Found" );
        }

        // 再次创建查询
        s3Client.createBucket( bucketName );
        headBucketResult = s3Client.headBucket( request );
        checkWs( headBucketResult.getBucketRegion() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }
        s3Client.shutdown();
    }

    private void checkWs( String wsName ) throws ScmException {
        try ( ScmSession session = ScmSessionUtils
                .createSession( ScmInfo.getSite() ) ;
                ScmCursor< ScmWorkspaceInfo > cursor = ScmFactory.Workspace
                        .listWorkspace( session ) ;) {
            boolean flag = false;
            while ( cursor.hasNext() ) {
                if ( wsName.equals( cursor.getNext().getName() ) ) {
                    flag = true;
                    break;
                }
            }
            Assert.assertTrue( flag );
        }
    }
}
