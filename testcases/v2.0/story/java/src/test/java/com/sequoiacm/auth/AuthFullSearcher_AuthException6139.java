package com.sequoiacm.auth;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption 创建全文检索(用户拥有 read 权限 、 用户拥有 all权限 、 删除用户不存在的角色 、 删除不存在的用户 、
 *              创建同名用户依次赋予无权限, read 权限, all 权限 5 种场景)
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthFullSearcher_AuthException6139 extends TestScmBase {
    private String wsName = "AuthFullSearcher6139WsName";
    private ScmWorkspace ws;
    private ScmSession session;
    private ScmSession userSession;
    private SiteWrapper rootSite;
    private ScmUser user;
    private ScmRole role;
    private ScmResource resource;
    private String userName = "AuthFullSearcher6139UserName";
    private String passwd = "AuthFullSearcher6139Pwd";
    private String roleName = "AuthFullSearcher6139RoleName";

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
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
            if ( userSession != null ) {
                userSession.close();
            }
        }
    }

    private void cleanEnv() throws Exception {
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

    private void testReCreateUser() throws ScmException {
        ScmFactory.User.deleteUser( session, userName );
        user = ScmFactory.User.createUser( session, userName,
                ScmUserPasswordType.LOCAL, passwd );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
        ws = ScmFactory.Workspace.getWorkspace( wsName, userSession );
        
        reCreateUserNoPrivilege();
        ScmFactory.Role.revokePrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        
        reCreateUserReadPrivilege();
        reCreateUserAllPrivilege();
    }

    private void reCreateUserReadPrivilege() throws ScmException {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.READ );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );

        try {
            createFullSearcher( "TestReCreateUser_ReadPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void reCreateUserAllPrivilege() throws ScmException {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        createFullSearcher( "TestReCreateUser_AllPrivilege" );
    }

    private void reCreateUserNoPrivilege() throws ScmException {
        ws = ScmFactory.Workspace.getWorkspace( wsName, userSession );
        try {
            createFullSearcher( "TestReCreateUser_NoPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
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

    private void testAllPrivilege() throws ScmException, InterruptedException {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );

        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        createFullSearcher( "testAllPrivilege" );
    }

    private void testReadPrivilege() throws ScmException, InterruptedException {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.READ );

        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );

        try {
            createFullSearcher( "testReadPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void createFullSearcher( String title )
            throws ScmException {
        BasicBSONObject fulltextMatcher = new BasicBSONObject(
                FieldName.FIELD_CLFILE_FILE_TITLE, title );
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                fulltextMatcher, ScmFulltextMode.async ) );
        ScmFactory.Fulltext.getIndexInfo( ws );
        ScmFactory.Fulltext.dropIndex( ws );
    }

    private void prepare() throws Exception {
        ws = ScmWorkspaceUtil.createWS( session, wsName, rootSite.getSiteId() );
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
