package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4277:用户存在ws权限，对ws下的桶执行文件操作
 * @Author YiPan
 * @CreateDate 2022/5/19
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4277 extends TestScmBase {
    private final String bucketName = "bucket4277";
    private final String objectName = "object4277";
    private final String content = "content4277";
    private ScmSession adminSession;
    private ScmSession newUserSession;
    private final String username = "user4277";
    private final String password = "passwd4277";
    private final String roleName = "ROLE_WS4277";
    private AmazonS3 s3Client = null;
    private String[] accessKeys = null;
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
        accessKeys = ScmAuthUtils.refreshAccessKey( adminSession, username,
                password, null );
        s3Client = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );

        // 清理创建bucket
        S3Utils.clearBucket( adminSession, s3WorkSpaces, bucketName );
        ScmFactory.Bucket.createBucket(
                ScmFactory.Workspace.getWorkspace( s3WorkSpaces, adminSession ),
                bucketName );
    }

    //SEQUOIACM-1146
    @Test(enabled = false)
    public void test() throws Exception {
        // 无权限创建
        checkCreateWithNoPrivilege();

        // 无权限创建
        ScmBucket bucket = ScmFactory.Bucket.getBucket( adminSession,
                bucketName );
        ScmFile file = bucket.createFile( objectName );
        file.save();
        checkUpdateWithNoPrivilege();

        // 无权限查询
        checkGetObjectWithNoPrivilege();

        // 无权限删除
        checkDeleteWithNoPrivilege();

        // 有权限删除、创建、查询、更新
        ScmAuthUtils.alterUser( adminSession, s3WorkSpaces, user, role,
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( accessKeys, s3WorkSpaces );
        s3Client.deleteObject( bucketName, objectName );
        s3Client.putObject( bucketName, objectName, content );
        s3Client.getObject( bucketName, objectName );
        s3Client.putObject( bucketName, objectName, content );
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

    private void checkCreateWithNoPrivilege() {
        try {
            s3Client.putObject( bucketName, objectName, content );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                throw e;
            }
        }
    }

    private void checkUpdateWithNoPrivilege() {
        try {
            s3Client.putObject( bucketName, objectName, content );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                throw e;
            }
        }
    }

    private void checkGetObjectWithNoPrivilege() {
        try {
            s3Client.getObject( bucketName, objectName );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                throw e;
            }
        }
    }

    private void checkDeleteWithNoPrivilege() {
        try {
            s3Client.deleteObject( bucketName, objectName );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                throw e;
            }
        }
    }
}