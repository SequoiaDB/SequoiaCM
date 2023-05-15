package com.sequoiacm.auth;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-6130:使用 Java 驱动进行操作桶和对象（基础场景、删除用户角色、删除用户、删除资源 4 中场景）
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthJavaDrive_AuthNormal6130 extends TestScmBase {
    private String wsName = "AuthJavaDrive6130WsName";
    private ScmWorkspace ws;
    private ScmSession session;
    private ScmSession userSession;
    private SiteWrapper rootSite;
    private ScmUser user;
    private ScmRole role;
    private ScmResource resource;
    private String userName = "AuthJavaDrive6130UserName";
    private String passwd = "AuthJavaDrive6130Pwd";
    private String roleName = "AuthJavaDrive6130RoleName";
    private String fileName = "AuthJavaDrive6130FileName";
    private String bucketName = "authjavadrive6130bucket";
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
        testNormal();
        testDeleteUserRole();
        testDeleteUser();
        testDeleteRole();
        testDeleteResource();
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            cleanEnv();
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( userSession != null ) {
                userSession.close();
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

    private void testDeleteResource() throws Exception {
        role = ScmFactory.Role.createRole( session, roleName, null );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        try {
            javaDriveCreateBucketAndUploadFile(
                    bucketName + "-testdeleteresource",
                    fileName + "_TestDeleteResource" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.WORKSPACE_NOT_EXIST
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteRole() throws Exception {
        user = ScmFactory.User.createUser( session, userName,
                ScmUserPasswordType.LOCAL, passwd );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
        ws = ScmFactory.Workspace.getWorkspace( wsName, userSession );

        javaDriveCreateBucketAndUploadFile( bucketName + "-testdeleterole",
                fileName + "testDeleteRole" );
        ScmFactory.Role.deleteRole( session, roleName );

        try {
            javaDriveCreateBucketAndUploadFile( bucketName + "-testdeleterole",
                    fileName + "_TestDeleteRole" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteUser() throws Exception {
        ScmFactory.User.deleteUser( session, userName );

        try {
            javaDriveCreateBucketAndUploadFile( bucketName + "-testdeleteuser",
                    fileName + "_TestDeleteUser" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteUserRole() throws Exception {
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().delRole( roleName ) );
        try {
            javaDriveCreateBucketAndUploadFile(
                    bucketName + "-testdeleteuserrole",
                    fileName + "_TestDeleteUserRole" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testNormal() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        javaDriveCreateBucketAndUploadFile( bucketName + "-testnormal",
                fileName + "_TestNormal" );
    }

    private void javaDriveCreateBucketAndUploadFile( String bucketName,
            String fileName ) throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        ScmBucket actBucket = ScmFactory.Bucket.getBucket( userSession,
                bucketName );
        Assert.assertEquals( actBucket.getName(), bucketName );

        ScmFile file = bucket.createFile( fileName );
        file.setContent( filePath );
        ScmId scmId = file.save();
        ScmFile actFile = bucket.getFile( fileName );
        Assert.assertEquals( actFile.toString(), file.toString() );
    }

    private void prepare() throws Exception {
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        ws = ScmWorkspaceUtil.createWS( session, wsName, rootSite.getSiteId() );
        ws.disableDirectory();

        resource = ScmResourceFactory.createWorkspaceResource( wsName );
        user = ScmAuthUtils.createUser( session, userName, passwd );
        role = ScmAuthUtils.createRole( session, roleName );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
        ws = ScmFactory.Workspace.getWorkspace( wsName, userSession );
    }

}
