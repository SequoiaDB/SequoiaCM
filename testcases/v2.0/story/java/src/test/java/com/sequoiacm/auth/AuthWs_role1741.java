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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1741:对角色授权，角色不存在
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_role1741 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthWs_role1741.class );
    private static final String NAME = "authws1741";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmRole role = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );

        // clean users and roles
        try {
            ScmFactory.User.deleteUser( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        try {
            ScmFactory.Role.deleteRole( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean roles in setUp, errorMsg = [" + e.getError() + "]" );
        }

        // prepare user
        this.createUserAndRole();
    }

    @Test
    private void test() throws ScmException, InterruptedException {
        // delete role
        ScmFactory.Role.deleteRole( session, NAME );

        // grantPrivilege
        ScmResource resource = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        try {
            ScmFactory.Role.grantPrivilege( session, role, resource,
                    ScmPrivilegeType.ALL );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "grantPrivilege but role not exist, errorMsg = ["
                    + e.getError() + "]" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME );
                try {
                    ScmFactory.Role.deleteRole( session, NAME );
                } catch ( ScmException e ) {
                    logger.info(
                            "delete not eixst role in tear down, errorMsg = "
                                    + e.getError() );
                }
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUserAndRole() throws ScmException {
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, PASSWORD );
        role = ScmFactory.Role.createRole( session, NAME, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

}
