package com.sequoiacm.auth;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-1480 :: 用户名/密码错误
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_LoginOnInfoError1480 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private SiteWrapper site;
    private ScmSession session;
    private String username = "LoginOnInfoError1480";
    private String passwd = "1480";
    private ScmUser user;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            user = ScmFactory.User
                    .createUser( session, username, ScmUserPasswordType.LOCAL,
                            passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmSession session = null;
        try {
            session = TestScmTools
                    .createSession( site, username, passwd + "_testInfoError" );
            Assert.fail( "exp login fail but act success,session = " +
                    session.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testInfoError() {
        ScmSession session = null;
        try {
            session = TestScmTools
                    .createSession( site, username, passwd + "_testInfoError" );
            Assert.fail( "exp login fail but act success,session = " +
                    session.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testInexistUser() {
        ScmSession session1 = null;
        try {
            ScmFactory.User.deleteUser( session, user );
            session1 = TestScmTools.createSession( site, username, passwd );
            Assert.fail( "exp login fail but act success,session1 = " +
                    session1.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session1 != null ) {
                session.close();
            }
        }

        runSuccess2 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess1 && runSuccess2 || TestScmBase.forceClear ) {
                if ( user != null ) {
                    ScmFactory.User.deleteUser( session, user );
                }
            }
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
