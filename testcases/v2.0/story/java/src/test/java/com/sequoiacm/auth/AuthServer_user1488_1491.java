package com.sequoiacm.auth;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @FileName SCM- SCM-1488:指定认证方式创建LOCAL用户 SCM-1491:重复创建用户
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1488_1491 extends TestScmBase {

    private static final Logger logger = Logger.getLogger(AuthServer_user1488_1491.class);
    private boolean runSuccess = false;

    private static SiteWrapper site = null;
    private ScmSession session = null;

    private static final String NAME = "auth1488";
    private static final String PASSWORD = NAME;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession(site);

            // clean new user
            try {
                ScmFactory.User.deleteUser(session, NAME);
            } catch (ScmException e) {
                logger.info("clean users in setUp, errorMsg = [" + e.getError() + "]");
            }
            try {
                ScmFactory.Role.deleteRole(session, NAME);
            } catch (ScmException e) {
                logger.info("clean roles in setUp, errorMsg = [" + e.getError() + "]");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException, InterruptedException {
        test_createUser();
        test_repeatCreateUser();
        test_deleteUser();
        runSuccess = true;
    }

    private void test_createUser() throws ScmException, InterruptedException {
        // create user and role
        ScmUser scmUser = ScmFactory.User.createUser(session, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
        ScmRole scmRole = ScmFactory.Role.createRole(session, NAME, "");
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole(scmRole);
        ScmFactory.User.alterUser(session, scmUser, modifier);

        // check user info
        scmUser = ScmFactory.User.getUser(session, NAME);
        Assert.assertEquals(scmUser.getUsername(), NAME);
        Assert.assertEquals(scmUser.getPasswordType(), ScmUserPasswordType.LOCAL);
        Assert.assertNotNull(scmUser.getUserId());
        Assert.assertEquals(scmUser.getRoles().size(), 1);
        Assert.assertTrue(scmUser.hasRole(NAME));
        Assert.assertTrue(scmUser.isEnabled());
    }

    private void test_repeatCreateUser() throws ScmException {
        try {
            ScmFactory.User.createUser(session, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
            Assert.fail("expect failed but actual succ.");
        } catch (ScmException e) {
            logger.info("repeat create user, errorMsg = [" + e.getError() + "]");
        }
    }

    private void test_deleteUser() throws ScmException {
        ScmFactory.User.deleteUser(session, NAME);
        try {
            ScmFactory.User.getUser(session, NAME);
            Assert.fail("expect failed but actual succ.");
        } catch (ScmException e) {
            logger.info("get user after delete, errorMsg = [" + e.getError() + "]");
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmFactory.Role.deleteRole(session, NAME);
            }
        } finally {
            if (null != session) {
                session.close();
            }
        }
    }
}
