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
 * @Descreption SCM-4260:使用SCM API指定不存在ws或user列取、统计桶
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4260 extends TestScmBase {
    private final String bucketNameBase = "bucket4260no";
    private List< String > bucketNames = new ArrayList<>();
    private ScmSession session;
    private final int bucketNum = 30;
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        dropBuckets();
    }

    @Test
    public void test() throws Exception {
        // 创建多个桶
        for ( int i = 0; i < bucketNum / 2; i++ ) {
            ScmFactory.Bucket.createBucket( ws, bucketNameBase + i );
            bucketNames.add( bucketNameBase + i );
        }

        // 指定错误ws名，列取、统计桶
        ScmCursor< ScmBucket > cursor = ScmFactory.Bucket.listBucket( session,
                "wrong", TestScmBase.scmUserName );
        Assert.assertFalse( cursor.hasNext() );
        cursor.close();
        Assert.assertEquals( ScmFactory.Bucket.countBucket( session, "test",
                TestScmBase.scmUserName ), 0 );

        // 指定错误user，列取、统计桶
        cursor = ScmFactory.Bucket.listBucket( session, s3WorkSpaces, "wrong" );
        Assert.assertFalse( cursor.hasNext() );
        cursor.close();
        Assert.assertEquals(
                ScmFactory.Bucket.countBucket( session, s3WorkSpaces, "wrong" ),
                0 );
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
        for ( int i = 0; i < bucketNum; i++ ) {
            try {
                ScmFactory.Bucket.deleteBucket( session, bucketNameBase + i );
            } catch ( ScmException e ) {
                if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                    throw e;
                }
            }
        }
    }
}