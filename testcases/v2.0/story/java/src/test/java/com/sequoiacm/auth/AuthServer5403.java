package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @descreption SCM-5403:普通用户修改自己的密码
 * @author ZhangYanan
 * @date 2022/11/05
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class AuthServer5403 extends TestScmBase {
    private static final String user1 = "user5403a";
    private static final String password1 = "password5043a";
    private static final String newPasswd1 = "newPassword5043a";
    private static final String user2 = "user5403b";
    private static final String password2 = "password5043b";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );

        ScmAuthUtils.deleteUser( session, user1 );
        ScmAuthUtils.deleteUser( session, user2 );
        createUser();
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        test_alterPasswd01();
        test_alterPasswd02();
        test_alterPasswdForOtherUser();
        runSuccess = true;
    }

    private void test_alterPasswd01() throws ScmException {
        // the same new and old password
        try ( ScmSession session = TestScmTools.createSession( site, user1,
                password1 )) {
            ScmUser scmUser = ScmFactory.User.getUser( session, user1 );

            ScmUserModifier modifier = new ScmUserModifier();
            modifier.setPassword( password1, password1 );
            ScmFactory.User.alterUser( session, scmUser, modifier );
        }

        ScmSession newSS = TestScmTools.createSession( site, user1, password1 );
        newSS.close();
    }

    private void test_alterPasswd02() throws ScmException {
        // the same new and old password
        try ( ScmSession session = TestScmTools.createSession( site, user1,
                password1 )) {

            ScmUser scmUser = ScmFactory.User.getUser( session, user1 );
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.setPassword( password1, newPasswd1 );
            ScmFactory.User.alterUser( session, scmUser, modifier );
        }

        // check results for alter password
        try {
            TestScmTools.createSession( site, user1, password1 );
            Assert.fail( "expect failed but actual success!" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw e;
            }
        }

        ScmSession newSS = TestScmTools.createSession( site, user1,
                newPasswd1 );
        newSS.close();
    }

    private void test_alterPasswdForOtherUser() throws ScmException {
        try ( ScmSession session = TestScmTools.createSession( site, user2,
                password2 )) {

            ScmUser scmUser = ScmFactory.User.getUser( session, user1 );
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.setPassword( password1, newPasswd1 );
            ScmFactory.User.alterUser( session, scmUser, modifier );
            Assert.fail( "expect failed but actual success!" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, user1 );
                ScmFactory.Role.deleteRole( session, user2 );
                ScmAuthUtils.deleteUser( session, user1 );
                ScmAuthUtils.deleteUser( session, user2 );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUser() throws ScmException {

        // create user1
        ScmUser scmUser = ScmFactory.User.createUser( session, user1,
                ScmUserPasswordType.LOCAL, password1 );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.createRole( session, user1, "" );
        modifier.addRole( user1 );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // create user2
        scmUser = ScmFactory.User.createUser( session, user2,
                ScmUserPasswordType.LOCAL, password2 );
        ScmFactory.Role.createRole( session, user2, "" );
        modifier = new ScmUserModifier();
        modifier.addRole( user2 );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }
}
