package com.sequoiacm.auth;

import java.io.IOException;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1788 :: CreateDirectoryResource1793参数校验
 * @author fanyu
 * @Date:2018年6月11日
 * @version:1.0
 */
public class AuthWs_Param_CreateDirectoryResource1793 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private String username = "AuhtWs_1793";
    private String rolename = "1793";
    private String passwd = "1793";
    private ScmUser user;
    private ScmRole role;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws InterruptedException, IOException {
        try {
            site = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( site );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWsIsNull() {
        ScmResource rs = ScmResourceFactory.createDirectoryResource( null,
                "/" );
        try {
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWsWithColon() {
        ScmResource rs = ScmResourceFactory
                .createDirectoryResource( wsp.getName() + ":", "/" );
        try {
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testDirIsNull() {
        ScmResource rs = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), null );
        try {
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testDirWithColon() {
        ScmResource rs = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), ":/" );
        try {
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testDirNotExist() {
        ScmResource rs = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), "/1793" );
        try {
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, ScmPrivilegeType privileges )
            throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
