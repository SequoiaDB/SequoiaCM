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
 * @DescreptionSCM-6129:使用 S3 SDK 进行创建桶和对象(基础场景、删除用户角色、删除用户、删除资源 4 中场景)
 * 
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthS3SDK_AuthNormal6129 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmUser user;
    private ScmRole role;
    private ScmResource resource;
    private String wsName = "AuthS3SDK6129WsName";
    private String bucketName = "auths3sdk6129bucket";
    private String userName = "AuthS3SDK6129UserName";
    private String passwd = "AuthS3SDK6129Pwd";
    private String roleName = "AuthS3SDK6129RoleName";
    private String fileName = "AuthS3SDK6129File";
    private ScmWorkspace ws;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;
    private String s3NoPermission = "AccessDenied";

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
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                cleanEnv();
            }
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

    private void testDeleteResource() throws Exception {
        role = ScmFactory.Role.createRole( session, roleName, null );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        try {
            S3sdkCreateBucketAndUploadFile( bucketName + "-testdeleteresource",
                    fileName + "_TestDeleteResource" );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception exception ) {
            Assert.assertEquals( exception.getErrorCode(), "NoSuchRegion" );
        }
    }

    private void testDeleteRole() throws Exception {
        user = ScmFactory.User.createUser( session, userName,
                ScmUserPasswordType.LOCAL, passwd );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        S3sdkCreateBucketAndUploadFile( bucketName + "-testdeleterole",
                fileName + "_TestDeleteRole" );
        ScmFactory.Role.deleteRole( session, roleName );
        try {
            S3sdkCreateBucketAndUploadFile( bucketName + "-testdeleterole",
                    fileName + "_TestDeleteRole" );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception ex ) {
            if ( !ex.getErrorCode().equals( s3NoPermission ) ) {
                throw ex;
            }
        }
    }

    private void testDeleteUser() throws Exception {
        ScmFactory.User.deleteUser( session, userName );
        try {
            S3sdkCreateBucketAndUploadFile( bucketName + "-testdeleteuser",
                    fileName + "_TestDeleteUser" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            Assert.assertEquals( ScmError.HTTP_NOT_FOUND,
                    exception.getError() );
        }
    }

    private void testDeleteUserRole() throws Exception {
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().delRole( roleName ) );
        try {
            S3sdkCreateBucketAndUploadFile( bucketName + "-testdeleteuserrole",
                    fileName + "_TestDeleteUserRole" );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception ex ) {
            if ( !ex.getErrorCode().equals( s3NoPermission ) ) {
                throw ex;
            }
        }
    }

    private void testNormal() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        S3sdkCreateBucketAndUploadFile( bucketName + "-testnormal",
                fileName + "_TestNormal" );
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
