package com.sequoiacm.auth.serial;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-6140:修改配置操作(用户拥有普通角色、用户拥有管理员角色、
 *              删除用户不存在的角色、删除不存在的用户、创建同名用户依次普通用户,管理员角色 5 种场景)
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthUpdateConf_AuthException6140 extends TestScmBase {
    private ScmSession session = null;
    private ScmSession userSession;
    private SiteWrapper rootSite = null;
    private ScmUser user;
    private String userName = "AuthUpdateConf6140UserName";
    private String passwd = "AuthUpdateConf6140Pwd";
    private String adminRoleName = "ROLE_AUTH_ADMIN";
    private String userRoleName = "AuthUpdateConf6140RoleName";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        testUserRole();
        testAdminRole();
        testDeleteUserNotExistRole();
        testDeleteNotExistUser();
        testReCreateUser();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                cleanEnv();
            }
        } finally {
            ConfUtil.deleteAuditConf( rootSite.getSiteServiceName() );
            if ( session != null ) {
                session.close();
            }
            if ( userSession != null ) {
                userSession.close();
            }
        }
    }

    private void cleanEnv() throws Exception {
        ScmAuthUtils.deleteUser( session, userName );
        try {
            ScmFactory.Role.deleteRole( session, userRoleName );
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
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
        reCreateUserAWithUserRole();
        reCreateUserAWithAdminRole();
    }

    private void reCreateUserAWithAdminRole() throws ScmException {
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( adminRoleName ) );
        updateConfig();
    }

    private void reCreateUserAWithUserRole() throws ScmException {
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( userRoleName ) );
        try {
            updateConfig();
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_FORBIDDEN
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testAdminRole() throws ScmException {
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( adminRoleName ) );
        updateConfig();
    }

    private void testUserRole() throws ScmException {
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( userRoleName ) );
        try {
            updateConfig();
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_FORBIDDEN
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
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().delRole( userRoleName + "_NotExist" ) );
    }

    private void updateConfig() throws ScmException {

        ScmConfigProperties conf = ScmConfigProperties.builder()
                .service( rootSite.getSiteServiceName() )
                .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                        "LOCAL" )
                .build();
        ScmUpdateConfResultSet ret = ScmSystem.Configuration
                .setConfigProperties( userSession, conf );
        Assert.assertEquals( ret.getFailures().size(), 0 );

    }

    private void prepare() throws ScmException {
        ScmAuthUtils.createRole( session, userRoleName );
        user = ScmAuthUtils.createUser( session, userName, passwd );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
    }

}
