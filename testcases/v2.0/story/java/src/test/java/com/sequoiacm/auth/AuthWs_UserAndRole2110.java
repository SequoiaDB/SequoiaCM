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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @FileName SCM-2110:删除用户关联的角色,删除用户
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_UserAndRole2110 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthWs_UserAndRole2110.class );
    private boolean runSuccess = false;

    private SiteWrapper site = null;
    private ScmSession session = null;

    private String name = "AuthWs_UserAndRole2110";
    private String passwd = "2110";
    private ScmRole role = null;
    private ScmUser user = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

        // clean users and roles
        try {
            ScmFactory.User.deleteUser( session, name );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        try {
            ScmFactory.Role.deleteRole( session, name );
        } catch ( ScmException e ) {
            logger.info(
                    "clean roles in setUp, errorMsg = [" + e.getError() + "]" );
        }

        // prepare user
        this.createUserAndRole();
    }

    @Test
    private void test() throws ScmException, InterruptedException {
        ScmFactory.Role.deleteRole( session, role );
        ScmFactory.User.deleteUser( session, user );
        check( user );
        runSuccess = true;
    }

    @AfterClass
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

    private void check( ScmUser user ) {
        try {
            ScmFactory.User.getUser( session, name );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void createUserAndRole() throws ScmException {
        user = ScmFactory.User.createUser( session, name,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( session, name, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
    }
}
