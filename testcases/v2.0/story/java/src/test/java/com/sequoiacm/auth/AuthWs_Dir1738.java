package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description:SCM-1738 :: 查询的目录权限没有匹配到记录
 * @author fanyu
 * @Date:2018年6月15日
 * @version:1.0
 */
public class AuthWs_Dir1738 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsR;
    private String username = "AuthWs_Dir1738";
    private String rolename = "ROLE_1738_R";
    private String passwd = "1738";
    private ScmUser user;
    private ScmRole role;
    private List< ScmResource > dirrsList = new ArrayList< ScmResource >();
    private String[] dirpaths = { "/AuthWs_Dir1738_A", "/AuthWs_Dir1738_B",
            "/AuthWs_Dir1738_C" };

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( site );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIncludeDirrsInHead() throws ScmException {
        // create dir that it's name contains dirPath's name.
        // eg:"/AuthWs_Dir1738_123" ,/AuthWs_Dir1738"
        String path = dirpaths[ 0 ] + "_" + UUID.randomUUID();
        ScmDirectory actDir = null;
        try {
            actDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.getInstance( wsR, path );
            Assert.fail(
                    "the user does not have priority to read dir= " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIncludeDirrsInMiddle() throws ScmException {
        // create dir that it's name is include dirPath's name.
        // eg:"/AuthWs_Dir1738_123" ,/AuthWs_Dir1738"
        String path = dirpaths[ 1 ] + "_" + UUID.randomUUID();
        ScmDirectory actDir = null;
        try {
            actDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.getInstance( wsR, path );
            Assert.fail(
                    "the user does not have priority to read dir= " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIncludeDirrsInTail() throws ScmException {
        // create dir that it's name is include dirPath's name.
        // eg:"/AuthWs_Dir1738_123" ,/AuthWs_Dir1738"
        String path = dirpaths[ 2 ] + "_" + UUID.randomUUID();
        ScmDirectory actDir = null;
        try {
            actDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.getInstance( wsR, path );
            Assert.fail(
                    "the user does not have priority to read dir= " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testUnrelated() throws ScmException {
        // create dir that it's independent of dirpath's name.
        // eg:"/B_AuthWs_Dir1738" ,/AuthWs_Dir1738"
        String path = "/B_AuthWs_Dir1738";
        ScmDirectory actDir = null;
        try {
            actDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.getInstance( wsR, path );
            Assert.fail(
                    "the user does not have priority to read dir= " + path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( actDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            for ( ScmResource rs : dirrsList ) {
                ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                        ScmPrivilegeType.READ );
            }
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            for ( String dirpath : dirpaths ) {
                ScmFactory.Directory.deleteInstance( wsA, dirpath );
            }
        } catch ( Exception e ) {
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
        ScmFactory.Role.grantPrivilege( session, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
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

    private void prepare() throws Exception {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            for ( String dirpath : dirpaths ) {
                ScmFactory.Directory.createInstance( wsA, dirpath );
                ScmResource dirrs = ScmResourceFactory
                        .createDirectoryResource( wsp.getName(), dirpath );
                dirrsList.add( dirrs );
                grantPriAndAttachRole( sessionA, dirrs, user, role,
                        ScmPrivilegeType.READ );
                ScmAuthUtils.checkPriority( site, username, passwd, role,
                        wsp.getName() );
            }
            sessionR = TestScmTools.createSession( site, username, passwd );
            wsR = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionR );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
