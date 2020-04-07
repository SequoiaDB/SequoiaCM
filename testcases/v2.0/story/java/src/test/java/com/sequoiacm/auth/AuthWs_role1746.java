package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @FileName SCM-1746:根目录有ALL的权限，授权给其它角色
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_role1746 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthWs_role1746.class );
    private static final String NAME = "authws1746";
    private static final String PASSWORD = NAME;
    private static final String DIR_PATH = "/";
    private static final int ROLE_NUM = 2;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private List< ScmRole > roles = new ArrayList<>();

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        // clean
        cleanEnv();
        // prepare user
        this.createUserAndRole();
    }

    @Test
    private void test() throws Exception {
        ScmResource resource = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), DIR_PATH );
        ScmFactory.Role.grantPrivilege( session, roles.get( 0 ), resource,
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriority( site, NAME, PASSWORD, roles.get( 0 ),
                wsp.getName() );
        ScmSession ss = null;
        try {
            ss = TestScmTools.createSession( site, NAME, PASSWORD );
            ScmFactory.Role.grantPrivilege( ss, roles.get( 1 ), resource,
                    ScmPrivilegeType.ALL );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "grantPrivilege but the user is not ADMIN_AUTH(role) or " +
                            "ws_ALL(privilege), errorMsg = ["
                            + e.getError() + "]" );
        } finally {
            if ( null != ss )
                ss.close();
        }

        // check results
        ScmRole role = roles.get( 0 );
        ScmCursor< ScmPrivilege > cursor = ScmFactory.Privilege
                .listPrivileges( session, role );
        Assert.assertTrue( cursor.hasNext() );

        role = roles.get( 1 );
        cursor = ScmFactory.Privilege.listPrivileges( session, role );
        Assert.assertFalse( cursor.hasNext() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME );
                for ( int i = 0; i < ROLE_NUM; i++ ) {
                    ScmFactory.Role.deleteRole( session, NAME + "_" + i );
                }
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUserAndRole() throws ScmException {
        ScmUser scmUser = ScmFactory.User
                .createUser( session, NAME, ScmUserPasswordType.LOCAL,
                        PASSWORD );

        ScmUserModifier modifier = new ScmUserModifier();
        for ( int i = 0; i < ROLE_NUM; i++ ) {
            String roleName = NAME + "_" + i;
            ScmRole role = ScmFactory.Role.createRole( session, roleName, "" );
            roles.add( role );
            modifier.addRole( roleName );
        }
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    private void cleanEnv() {
        // clean users and roles
        try {
            ScmFactory.User.deleteUser( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        for ( int i = 0; i < ROLE_NUM; i++ ) {
            try {
                ScmFactory.Role.deleteRole( session, NAME + "_" + i );
            } catch ( ScmException e ) {
                logger.info(
                        "clean roles in setUp, errorMsg = [" + e.getError() +
                                "]" );
            }
        }
    }
}
