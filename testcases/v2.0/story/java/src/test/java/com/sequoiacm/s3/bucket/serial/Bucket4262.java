package com.sequoiacm.s3.bucket.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @Descreption SCM-4262:创建删除桶，统计桶数量
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4262 extends TestScmBase {
    private final String bucketNameBase = "bucket4262no";
    private List< String > bucketNames = new ArrayList<>();
    private ScmSession session;
    private final int baseBucketNum = 30;
    private final int insBucketNum = 10;
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        dropBuckets();
    }

    @Test
    public void test() throws Exception {
        // 创建多个桶,统计桶数量
        for ( int i = 0; i < baseBucketNum; i++ ) {
            ScmFactory.Bucket.createBucket( ws, bucketNameBase + i );
            bucketNames.add( bucketNameBase + i );
        }
        checkBucketNum();

        // 删除部分桶,统计桶数量
        for ( int i = 0; i < baseBucketNum / 2; i++ ) {
            ScmFactory.Bucket.deleteBucket( session, bucketNameBase + i );
            bucketNames.remove( bucketNameBase + i );
        }
        checkBucketNum();

        // 再次创建部分桶,统计桶数量
        for ( int i = baseBucketNum; i < insBucketNum; i++ ) {
            ScmFactory.Bucket.createBucket( ws, bucketNameBase + i );
            bucketNames.add( bucketNameBase + i );
        }
        checkBucketNum();
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                dropBuckets();
            }
        } finally {
            session.close();
        }
    }

    private void dropBuckets() throws ScmException {
        for ( int i = 0; i < baseBucketNum + insBucketNum; i++ ) {
            try {
                ScmFactory.Bucket.deleteBucket( session, bucketNameBase + i );
            } catch ( ScmException e ) {
                if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                    throw e;
                }
            }
        }
    }

    private void checkBucketNum() throws ScmException {
        long actBucketNum = ScmFactory.Bucket.countBucket( session,
                s3WorkSpaces, TestScmBase.scmUserName );
        Assert.assertEquals( actBucketNum, bucketNames.size() );
    }
}