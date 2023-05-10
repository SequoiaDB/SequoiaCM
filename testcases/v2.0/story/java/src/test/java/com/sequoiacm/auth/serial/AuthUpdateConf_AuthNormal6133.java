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
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-6133:修改配置操作(基础场景、删除用户角色、删除用户 3 中场景)
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthUpdateConf_AuthNormal6133 extends TestScmBase {
    private ScmSession session = null;
    private ScmSession userSession;
    private SiteWrapper rootSite = null;
    private ScmUser user;
    private ScmRole role;
    private String userName = "AuthUpdateConf6133UserName";
    private String passwd = "AuthUpdateConf6133Pwd";
    private String roleName = "ROLE_AUTH_ADMIN";
    private boolean runSuccess = false;

    @Test(groups = { "twoSite", "fourSite" })
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        testNormal();
        testDeleteUserRole();
        testDeleteUser();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmAuthUtils.deleteUser( session, userName );
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

    private void testDeleteUser() throws Exception {
        ScmFactory.User.deleteUser( session, userName );
        try {
            updateConfig();
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
            updateConfig();
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_FORBIDDEN
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testNormal() throws Exception {
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        updateConfig();
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
        user = ScmAuthUtils.createUser( session, userName, passwd );
        role = ScmFactory.Role.getRole( session, roleName );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
    }

}
