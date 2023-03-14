package com.sequoiacm.auth;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @Description: SCM-1560 :: alterUser参数校验
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_Param_AlterUser1560 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private ScmUser user;
    private String username = "Param_AlterUser1560";
    private String passwd = "1560";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        try {
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testInexistUser() {
        String username = "AlterUser1560";
        try {
            ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            ScmFactory.User.deleteUser( session, username );
            ScmFactory.User.alterUser( session, user, new ScmUserModifier() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDelInexistRole() {
        String roleName = "AlterUser1560";
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.delRole( roleName );
            ScmFactory.User.alterUser( session, user, new ScmUserModifier() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testAddInexistRole() {
        String roleName = "AlterUser1560";
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole( roleName );
            ScmFactory.User.alterUser( session, user, modifier );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_BAD_REQUEST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testAddNullRole() {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRoleNames( null );
            ScmFactory.User.alterUser( session, user, modifier );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.User.deleteUser( session, username );
            if ( session != null ) {
                session.close();
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
