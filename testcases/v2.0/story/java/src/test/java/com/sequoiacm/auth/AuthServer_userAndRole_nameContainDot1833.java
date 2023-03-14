package com.sequoiacm.auth;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM- SCM-1488:指定认证方式创建LOCAL用户 SCM-1491:重复创建用户
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_userAndRole_nameContainDot1833 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_userAndRole_nameContainDot1833.class );
    private static final String NAME = "auth1833.a";
    private static final String PASSWORD = NAME;
    private static SiteWrapper site = null;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );
            wsp = ScmInfo.getWs();

            // clean new user
            try {
                ScmFactory.User.deleteUser( session, NAME );
            } catch ( ScmException e ) {
                logger.info( "clean users in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
            try {
                ScmFactory.Role.deleteRole( session, NAME );
            } catch ( ScmException e ) {
                logger.info( "clean roles in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        // create user and role
        ScmUser user = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, PASSWORD );
        ScmRole role = ScmFactory.Role.createRole( session, NAME, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );

        // check user info
        user = ScmFactory.User.getUser( session, NAME );
        Assert.assertEquals( user.getUsername(), NAME );
        Assert.assertTrue( user.hasRole( NAME ) );

        role = ScmFactory.Role.getRole( session, NAME );
        Assert.assertEquals( role.getRoleName(), "ROLE_" + NAME );

        // grant and revoke privilege
        ScmResource resource = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        ScmFactory.Role.revokePrivilege( session, role, resource,
                ScmPrivilegeType.ALL );

        // login and logout new user
        ScmSession newSS = ScmSessionUtils.createSession( site, NAME, PASSWORD );
        newSS.close();

        // delete user and role
        ScmFactory.User.deleteUser( session, NAME );
        ScmFactory.Role.deleteRole( session, NAME );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

}
