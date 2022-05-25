package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4276:用户存在ws部分权限，执行桶创建、删除操作
 * @Author YiPan
 * @CreateDate 2022/5/19
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4276 extends TestScmBase {
    private final String bucketName = "bucket4276";
    private ScmSession adminSession;
    private ScmSession newUserSession;
    private final String username = "user4276";
    private final String password = "passwd4276";
    private final String roleName = "ROLE_WS4276";
    private AmazonS3 s3Client = null;
    private ScmUser user = null;
    private ScmRole role = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        adminSession = TestScmTools.createSession( ScmInfo.getRootSite() );

        // 新建用户
        user = ScmAuthUtils.createUser( adminSession, username, password );
        newUserSession = TestScmTools.createSession( ScmInfo.getRootSite(),
                username, password );
        role = ScmAuthUtils.createRole( adminSession, roleName );

        // 创建新用户s3连接
        String[] accessKeys = ScmAuthUtils.refreshAccessKey( adminSession,
                username, password, null );
        s3Client = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );

        // 清理bucket
        S3Utils.clearBucket( adminSession, s3WorkSpaces, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 无权限创建
        try {
            s3Client.createBucket( bucketName );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                throw e;
            }
        }

        // 有权限创建
        ScmAuthUtils.alterUser( adminSession, s3WorkSpaces, user, role,
                ScmPrivilegeType.CREATE );
        checkS3Authorized( ScmPrivilegeType.CREATE );

        // 无权限删除
        try {
            s3Client.deleteBucket( bucketName );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                throw e;
            }
        }

        // 有权限删除
        ScmAuthUtils.alterUser( adminSession, s3WorkSpaces, user, role,
                ScmPrivilegeType.DELETE );
        ScmAuthUtils.alterUser( adminSession, s3WorkSpaces, user, role,
                ScmPrivilegeType.READ );
        checkS3Authorized( ScmPrivilegeType.DELETE );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( adminSession, s3WorkSpaces, bucketName );
                ScmFactory.Role.deleteRole( adminSession, roleName );
                ScmFactory.User.deleteUser( adminSession, username );
            }
        } finally {
            s3Client.shutdown();
            adminSession.close();
            newUserSession.close();
        }
    }

    private void checkS3Authorized( ScmPrivilegeType type ) throws Exception {
        boolean flag = true;
        int times = 0;
        while ( flag ) {
            try {
                if ( type.equals( ScmPrivilegeType.CREATE ) ) {
                    s3Client.createBucket( bucketName );
                    Assert.assertTrue(
                            s3Client.doesBucketExistV2( bucketName ) );
                    flag = false;
                } else if ( type.equals( ScmPrivilegeType.DELETE ) ) {
                    s3Client.deleteBucket( bucketName );
                    Assert.assertFalse(
                            s3Client.doesBucketExistV2( bucketName ) );
                    flag = false;
                } else {
                    throw new Exception(
                            "ScmPrivilegeType only support create/delete" );
                }
            } catch ( AmazonS3Exception e ) {
                if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                    throw e;
                }
                times++;
                Thread.sleep( 1000 );
                if ( times > 60 ) {
                    throw new Exception( "check " + type + " time out" );
                }
            }
        }
    }
}