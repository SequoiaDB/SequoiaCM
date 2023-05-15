package com.sequoiacm.auth;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmAccesskeyInfo;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-6136:使用 S3 SDK 进行创建桶和对象(用户拥有 read 权限、用户拥有 all
 *              权限、删除用户不存在的角色、删除不存在的用户、创建同名用户依次赋予无权限,read 权限,all 权限 5种场景)
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthS3SDK_AuthException6136 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmUser user;
    private ScmRole role;
    private ScmResource resource;
    private String wsName = "AuthS3SDK6136WsName";
    private String bucketName = "auths3sdk-6136bucket";
    private String userName = "AuthS3SDK6136UserName";
    private String passwd = "AuthS3SDK6136Pwd";
    private String roleName = "AuthS3SDK6136RoleName";
    private String fileName = "AuthS3SDK6136File";
    private String s3NoPermission = "AccessDenied";
    private ScmWorkspace ws;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + fileName + fileSize + ".txt";
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        testReadPrivilege();
        testAllPrivilege();
        testDeleteUserNotExistRole();
        testDeleteNotExistUser();
        testReCreateUser();
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            cleanEnv();
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void cleanEnv() throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmAuthUtils.deleteUser( session, userName );
        try {
            ScmFactory.Role.deleteRole( session, roleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    private void testReCreateUser() throws Exception {
        ScmFactory.User.deleteUser( session, userName );
        user = ScmFactory.User.createUser( session, userName,
                ScmUserPasswordType.LOCAL, passwd );
        reCreateUserNoPrivilege();

        ScmFactory.Role.revokePrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        reCreateUserReadPrivilege();
        reCreateUserAllPrivilege();
    }

    private void reCreateUserAllPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        S3sdkCreateBucketAndUploadFile(
                bucketName + "-recreateuserallprivilege",
                fileName + "_ReCreateUserAllPrivilege" );
    }

    private void reCreateUserReadPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.READ );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        try {
            S3sdkCreateBucketAndUploadFile(
                    bucketName + "-recreateuserreadprivilege",
                    fileName + "_ReCreateUserReadPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception ex ) {
            if ( !ex.getErrorCode().equals( s3NoPermission ) ) {
                throw ex;
            }
        }
    }

    private void reCreateUserNoPrivilege() throws Exception {
        try {
            S3sdkCreateBucketAndUploadFile(
                    bucketName + "-recreateusernoprivilege",
                    fileName + "_ReCreateUserNoPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception ex ) {
            if ( !ex.getErrorCode().equals( s3NoPermission ) ) {
                throw ex;
            }
        }
    }

    private void testDeleteNotExistUser() throws ScmException {
        try {
            ScmFactory.User.deleteUser( session, userName + "_NotExist" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_NOT_FOUND
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteUserNotExistRole() throws ScmException {
        try {
            user = ScmFactory.User.alterUser( session, user,
                    new ScmUserModifier().delRole( role + "_NotExist" ) );
        } catch ( ScmException ex ) {
            if ( ex.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw ex;
            }
        }
    }

    private void testAllPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        S3sdkCreateBucketAndUploadFile( bucketName + "-testallprivilege",
                fileName + "_TestAllPrivilege" );
    }

    private void testReadPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.READ );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        try {
            S3sdkCreateBucketAndUploadFile( bucketName + "-testreadprivilege",
                    fileName + "_TestReadPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception ex ) {
            if ( !ex.getErrorCode().equals( s3NoPermission ) ) {
                throw ex;
            }
        }
    }

    private void S3sdkCreateBucketAndUploadFile( String bucketName, String key )
            throws Exception {
        ScmAccesskeyInfo accesskeyInfo = ScmFactory.S3
                .refreshAccesskey( session, userName, passwd );
        s3Client = S3Utils.buildS3Client( accesskeyInfo.getAccesskey(),
                accesskeyInfo.getSecretkey() );
        Bucket bucket = s3Client
                .createBucket( new CreateBucketRequest( bucketName, wsName ) );
        Assert.assertEquals( bucketName, bucket.getName() );
        s3Client.putObject( bucketName, key, new File( filePath ) );
        S3Object s3Object = s3Client.getObject( bucketName, key );
        Assert.assertTrue( key.equals( s3Object.getKey() )
                && bucketName.equals( s3Object.getBucketName() ) );
    }

    private void prepare() throws Exception {
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        ws = ScmWorkspaceUtil.createWS( session, wsName, rootSite.getSiteId() );
        ws.disableDirectory();

        resource = ScmResourceFactory.createWorkspaceResource( wsName );
        user = ScmAuthUtils.createUser( session, userName, passwd );
        role = ScmAuthUtils.createRole( session, roleName );
    }

}
