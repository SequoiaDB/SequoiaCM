package com.sequoiacm.s3.bucket.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @Descreption SCM-4258:使用SCM API指定ws列取、统计桶
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4258 extends TestScmBase {
    private final String bucketNameBase = "bucket4258no";
    private final String wsName = "ws4258";
    private List< String > newWsBuckets = new ArrayList<>();
    private List< String > s3WsBuckets = new ArrayList<>();
    private ScmSession adminSession;
    private ScmSession newUserSession;
    private final int bucketNum = 30;
    private final String username = "user4258";
    private final String password = "passwd4258";
    private ScmUser user;
    private ScmWorkspace s3Ws;
    private ScmWorkspace newWs;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        adminSession = TestScmTools.createSession( ScmInfo.getRootSite() );
        // 新建用户赋予默认s3工作区权限
        ScmAuthUtils.createAdminUserGrant( adminSession, s3WorkSpaces, username,
                password );
        newUserSession = TestScmTools.createSession( ScmInfo.getRootSite(),
                username, password );
        // 清理环境
        dropBuckets();
        createWs( wsName );
        // 赋予新建用户新建ws权限
        user = ScmFactory.User.getUser( adminSession, username );
        ScmAuthUtils.alterUser( adminSession, wsName, user,
                ScmFactory.Role.getRole( adminSession, "ROLE_AUTH_ADMIN" ),
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( newUserSession, wsName );

        s3Ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                newUserSession );
        newWs = ScmFactory.Workspace.getWorkspace( wsName, newUserSession );
    }

    @Test
    public void test() throws Exception {
        // 在不同ws下创建多个桶
        for ( int i = 0; i < bucketNum / 2; i++ ) {
            ScmFactory.Bucket.createBucket( s3Ws, bucketNameBase + i );
            s3WsBuckets.add( bucketNameBase + i );
        }
        for ( int i = bucketNum / 2; i < bucketNum; i++ ) {
            ScmFactory.Bucket.createBucket( newWs, bucketNameBase + i );
            newWsBuckets.add( bucketNameBase + i );
        }

        // 列取、统计新建ws下的桶
        ScmCursor< ScmBucket > newWsBucketScmCursor = ScmFactory.Bucket
                .listBucket( newUserSession, wsName, user.getUsername() );
        S3Utils.checkBucketList( newWsBucketScmCursor, newWsBuckets, false );
        Assert.assertEquals( ScmFactory.Bucket.countBucket( newUserSession,
                wsName, user.getUsername() ), bucketNum / 2 );

        // 列取、统计s3 ws下的桶
        ScmCursor< ScmBucket > s3WsBucketScmCursor = ScmFactory.Bucket
                .listBucket( newUserSession, s3WorkSpaces, user.getUsername() );
        S3Utils.checkBucketList( s3WsBucketScmCursor, s3WsBuckets, false );
        Assert.assertEquals( ScmFactory.Bucket.countBucket( newUserSession,
                wsName, user.getUsername() ), bucketNum / 2 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                dropBuckets();
                ScmFactory.Workspace.deleteWorkspace( adminSession, wsName,
                        true );
                ScmFactory.User.deleteUser( adminSession, username );
            }
        } finally {
            newUserSession.close();
            adminSession.close();
        }
    }

    private void createWs( String wsName ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, adminSession );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setEnableDirectory( false );
        conf.setName( wsName );
        ScmWorkspaceUtil.createWS( adminSession, conf );
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