package com.sequoiacm.s3.bucket;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4254:SCM API查询桶元数据
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4254 extends TestScmBase {
    private final String bucketName = "bucket4254";
    private ScmSession session;
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 创建桶
        ScmFactory.Bucket.createBucket( ws, bucketName );

        // 查询桶元数据
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        Assert.assertEquals( bucket.getName(), bucketName );
        Assert.assertEquals( bucket.getWorkspace(), s3WorkSpaces );
        Assert.assertEquals( bucket.getCreateUser(), TestScmBase.scmUserName );
        Assert.assertNotNull( bucket.getCreateTime() );
        ScmBucket queryBucket = queryBucketById( bucket.getId() );
        Assert.assertEquals( queryBucket.getName(), bucketName );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
            }
        } finally {
            session.close();
        }
    }

    private ScmBucket queryBucketById( long id ) throws ScmException {
        BSONObject bsonObject = ScmQueryBuilder
                .start( ScmAttributeName.Bucket.ID ).is( id ).get();
        ScmCursor< ScmBucket > scmBucketScmCursor = ScmFactory.Bucket
                .listBucket( session, bsonObject, null, 0, -1 );
        ScmBucket bucket = null;
        try {
            bucket = scmBucketScmCursor.getNext();
        } finally {
            scmBucketScmCursor.close();
        }
        return bucket;
    }
}