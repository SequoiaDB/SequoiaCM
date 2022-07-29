package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-4279:用户不存在ws和桶权限，列取桶
 * @Author YiPan
 * @CreateDate 2022/5/19
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4279 extends TestScmBase {
    private final String bucketName = "bucket4279no";
    private final String wsName = "ws4279";
    private List< String > publicBuckets;
    private ScmSession session;
    private ScmSession newUserSession;
    private final String username = "user4279";
    private final String password = "passwd4279";
    private final String roleName = "ROLE_WS4279";
    private final int bucketNum = 10;
    private List< String > allBucketNames = new ArrayList<>();
    private AmazonS3 newS3Client = null;
    private AmazonS3 s3Client = null;
    private String[] accessKeys = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        publicBuckets = S3Utils.getPublicBuckets();
        session = TestScmTools.createSession( ScmInfo.getRootSite() );

        // 创建新ws、新用户并赋权
        ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmUser user = ScmAuthUtils.createUser( session, username, password );
        ScmRole role = ScmAuthUtils.createRole( session, roleName );
        ScmAuthUtils.alterUser( session, wsName, user, role,
                ScmPrivilegeType.ALL );

        // 创建新用户连接，校验赋权结果
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        newS3Client = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );
        ScmAuthUtils.checkPriorityByS3( accessKeys, wsName );
        newUserSession = TestScmTools.createSession( ScmInfo.getSite(),
                username, password );
        ScmAuthUtils.checkPriorityByS3( newUserSession, wsName );

        // 清理并创建bucket
        for ( int i = 0; i < bucketNum; i++ ) {
            S3Utils.clearBucket( newS3Client, bucketName + i );
            newS3Client.createBucket( bucketName + i, wsName );
            allBucketNames.add( bucketName + i );
        }
        // 管理员用户创建连接
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void test() throws Exception {
        // s3接口列取桶
        List< Bucket > buckets = s3Client.listBuckets();

        // 校验(s3只能列取自己的桶)
        List< String > actBucketNames = new ArrayList<>();
        for ( Bucket bucket : buckets ) {
            actBucketNames.add( bucket.getName() );
        }
        Assert.assertEquals( actBucketNames.size() - publicBuckets.size(), 0,
                actBucketNames.toString() );

        // scm api列取桶
        actBucketNames = new ArrayList<>();
        ScmCursor< ScmBucket > cursor = ScmFactory.Bucket
                .listBucket( newUserSession, null, null, 0, -1 );
        while ( cursor.hasNext() ) {
            actBucketNames.add( cursor.getNext().getName() );
        }
        // 排除公共桶干扰
        actBucketNames.removeAll( publicBuckets );
        // scm api可列取所有用户的桶
        Assert.assertEqualsNoOrder( actBucketNames.toArray(),
                allBucketNames.toArray() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                for ( int i = 0; i < bucketNum; i++ ) {
                    S3Utils.clearBucket( newS3Client, bucketName + i );
                }
                ScmWorkspaceUtil.deleteWs( wsName, session );
                ScmFactory.Role.deleteRole( session, roleName );
                ScmFactory.User.deleteUser( session, username );
            }
        } finally {
            newS3Client.shutdown();
            s3Client.shutdown();
            session.close();
            newUserSession.close();
        }
    }
}