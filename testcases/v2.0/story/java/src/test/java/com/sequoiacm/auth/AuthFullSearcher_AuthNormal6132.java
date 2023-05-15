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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-6132:创建全文检索(基础场景、删除用户角色、删除用户、删除资源 4 中场景)
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthFullSearcher_AuthNormal6132 extends TestScmBase {
    private String wsName = "AuthFullSearcher6132WsName";
    private ScmWorkspace ws;
    private ScmSession session;
    private ScmSession userSession;
    private SiteWrapper rootSite;
    private ScmUser user;
    private ScmRole role;
    private ScmResource resource;
    private String userName = "AuthFullSearcher6132UserName";
    private String passwd = "AuthFullSearcherAuth6132Pwd";
    private String roleName = "AuthFullSearcher6132RoleName";

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
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
            createFullSearcher( "testDeleteResource" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.WORKSPACE_NOT_EXIST
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteRole() throws ScmException {
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
        createFullSearcher( "testDeleteRole" );
        ScmFactory.Role.deleteRole( session, roleName );
        try {
            createFullSearcher( "testDeleteRole" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteUser() throws ScmException {
        ScmFactory.User.deleteUser( session, userName );
        try {
            createFullSearcher( "testDeleteUser" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteUserRole() throws ScmException {
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().delRole( roleName ) );
        try {
            createFullSearcher( "testDeleteUserRole" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testNormal() throws ScmException, InterruptedException {

        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        createFullSearcher( "testNormal" );
    }

    private void createFullSearcher( String title ) throws ScmException {
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
