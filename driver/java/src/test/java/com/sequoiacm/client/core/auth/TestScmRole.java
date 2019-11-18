package com.sequoiacm.client.core.auth;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmRole extends ScmTestBase {
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
    public void testRole() throws ScmException {
        String roleName = "ROLE_test_";
        String desc = "role test";

        try {
            ScmFactory.Role.deleteRole(session, roleName);
        }
        catch (Exception e) {
            // ignore
        }

        ScmRole role = ScmFactory.Role.createRole(session, roleName, desc);
        assertEquals(role.getRoleName(), roleName);
        assertEquals(role.getDescription(), desc);

        ScmRole role1 = ScmFactory.Role.getRole(session, roleName);
        assertEquals(role1, role);
        assertEquals(role1.getRoleName(), role.getRoleName());
        assertEquals(role1.getDescription(), role.getDescription());

        ScmCursor<ScmRole> cursor = null;
        try {
            cursor = ScmFactory.Role.listRoles(session);
            assertTrue(cursor.hasNext());
            boolean hasRole = false;
            while (cursor.hasNext()) {
                ScmRole role2 = cursor.getNext();
                if (role2.getRoleName().equals(roleName)) {
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

        ScmFactory.Role.deleteRole(session, roleName);
    }
}
