package com.sequoiacm.client.core.auth;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmUser extends ScmTestBase {
    private ScmSession session;

    @BeforeClass
    public void setUpTestCase() throws ScmException {
        session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(url, user, password));
    }

    @AfterClass
    public void tearDownTestCase() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void testScmUser() throws ScmException {
        String username = "user_test";
        String password = "test";
        String roleName = "ROLE_test_";
        ScmUserPasswordType passwordType = ScmUserPasswordType.LOCAL;

        try {
            ScmFactory.Role.deleteRole(session, roleName);
        }
        catch (Exception e) {
            // ignore
        }

        try {
            ScmFactory.User.deleteUser(session, username);
        }
        catch (Exception e) {
            // ignore
        }

        ScmRole role = ScmFactory.Role.createRole(session, roleName, "test");
        assertEquals(role.getRoleName(), roleName);

        ScmUser user = ScmFactory.User.createUser(session, username, passwordType, password);
        assertEquals(user.getUsername(), username);
        assertEquals(user.getPasswordType(), passwordType);
        assertTrue(user.isEnabled());
        assertTrue(user.getRoles().isEmpty());

        ScmUser user1 = ScmFactory.User.getUser(session, username);
        assertEquals(user1, user);
        assertEquals(user1.getPasswordType(), user.getPasswordType());
        assertEquals(user1.getUsername(), user.getUsername());
        assertEquals(user1.getRoles(), user.getRoles());

        BSONObject matcher = new BasicBSONObject();
        matcher.put("password_type", ScmUserPasswordType.LOCAL);
        matcher.put("enabled", true);
        ScmCursor<ScmUser> cursor = null;
        try {
            cursor = ScmFactory.User.listUsers(session, matcher);
            assertTrue(cursor.hasNext());
            boolean hasRole = false;
            while (cursor.hasNext()) {
                ScmUser user2 = cursor.getNext();
                if (user2.getUsername().equals(username)) {
                    hasRole = true;
                }
            }
            assertTrue(hasRole);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ScmUserModifier modifier = new ScmUserModifier().setEnabled(false).setCleanSessions(true)
                .addRole("AUTH_ADMIN").addRole(role);
        ScmUser user2 = ScmFactory.User.alterUser(session, user, modifier);
        assertEquals(user2.getUserId(), user.getUserId());
        assertEquals(user2.getUsername(), user.getUsername());
        assertFalse(user2.isEnabled());
        assertTrue(user2.hasRole(role));
        assertTrue(user2.hasRole("AUTH_ADMIN"));

        ScmFactory.User.deleteUser(session, username);
    }
}
