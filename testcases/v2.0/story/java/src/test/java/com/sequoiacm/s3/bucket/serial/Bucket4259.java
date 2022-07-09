package com.sequoiacm.s3.bucket.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @Descreption SCM-4259:使用SCM API指定user列取、统计桶
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4259 extends TestScmBase {
    private final String bucketNameBase = "bucket4259no";
    private List< String > adminUserBuckets = new ArrayList<>();
    private List< String > newUserBuckets = new ArrayList<>();
    private ScmSession adminSession;
    private ScmSession newUserSession;
    private final int bucketNum = 30;
    private final String username = "user4259";
    private final String password = "passwd4259";
    private ScmWorkspace adminUserWs;
    private ScmWorkspace newUserWs;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        adminSession = TestScmTools.createSession( ScmInfo.getRootSite() );
        // 新建用户赋予默认s3工作区权限
        ScmAuthUtils.createAdminUserGrant( adminSession, s3WorkSpaces, username,
                password );
        newUserSession = TestScmTools.createSession( ScmInfo.getRootSite(),
                username, password );
        ScmAuthUtils.checkPriorityByS3( newUserSession, s3WorkSpaces );
        adminUserWs = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                adminSession );
        newUserWs = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                newUserSession );
        // 清理环境
        dropBuckets();
    }

    @Test
    public void test() throws Exception {
        // 在不同ws下创建多个桶
        for ( int i = 0; i < bucketNum / 2; i++ ) {
            ScmFactory.Bucket.createBucket( adminUserWs, bucketNameBase + i );
            adminUserBuckets.add( bucketNameBase + i );
        }
        for ( int i = bucketNum / 2; i < bucketNum; i++ ) {
            ScmFactory.Bucket.createBucket( newUserWs, bucketNameBase + i );
            newUserBuckets.add( bucketNameBase + i );
        }

        // 列取、统计admin user下的桶
        ScmCursor< ScmBucket > newWsBucketScmCursor = ScmFactory.Bucket
                .listBucket( newUserSession, s3WorkSpaces,
                        TestScmBase.scmUserName );
        S3Utils.checkBucketList( newWsBucketScmCursor, adminUserBuckets,
                false );
        Assert.assertEquals( ScmFactory.Bucket.countBucket( newUserSession,
                s3WorkSpaces, TestScmBase.scmUserName ), bucketNum / 2 );

        // 列取、统计新建user下的桶
        ScmCursor< ScmBucket > s3WsBucketScmCursor = ScmFactory.Bucket
                .listBucket( newUserSession, s3WorkSpaces, username );
        S3Utils.checkBucketList( s3WsBucketScmCursor, newUserBuckets, false );
        Assert.assertEquals( ScmFactory.Bucket.countBucket( newUserSession,
                s3WorkSpaces, username ), bucketNum / 2 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                dropBuckets();
                ScmFactory.User.deleteUser( adminSession, username );
            }
        } finally {
            newUserSession.close();
            adminSession.close();
        }
    }

    private void dropBuckets() throws ScmException {
        for ( int i = 0; i < bucketNum; i++ ) {
            try {
                ScmFactory.Bucket.deleteBucket( newUserSession,
                        bucketNameBase + i );
            } catch ( ScmException e ) {
                if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                    throw e;
                }
            }
        }
    }
}