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
 * @Description:SCM-1558 :: createUser参数校验
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_Param_CreateUser1558 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private ScmUser user;
    private String username = "CreateUser1558";
    private String passwd = "1558";

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
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testPasswdIsNull() throws ScmException {
        try {
            ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_BAD_REQUEST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testPasswdType() throws ScmException {
        try {
            ScmFactory.User.createUser( session, username, null, passwd );
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
            ScmFactory.User.deleteUser( session, user );
            if ( session != null ) {
                session.close();
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
