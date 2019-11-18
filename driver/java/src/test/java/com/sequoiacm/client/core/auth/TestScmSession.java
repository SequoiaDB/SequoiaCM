package com.sequoiacm.client.core.auth;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmSession extends ScmTestBase {
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
    public void testSession() throws ScmException {
        ScmSessionInfo sessionInfo = ScmFactory.Session.getSessionInfo(session,
                session.getSessionId());
        assertEquals(sessionInfo.getSessionId(), session.getSessionId());
        assertEquals(sessionInfo.getUsername(), session.getUser());

        ScmCursor<ScmSessionInfo> cursor1 = null;
        try {
            cursor1 = ScmFactory.Session.listSessions(session, user);
            while (cursor1.hasNext()) {
                ScmSessionInfo si = cursor1.getNext();
                assertEquals(si.getUsername(), session.getUser());
            }
        }
        finally {
            if (null != cursor1) {
                cursor1.close();
            }
        }

        ScmCursor<ScmSessionInfo> cursor2 = null;
        try {
            cursor2 = ScmFactory.Session.listSessions(session);
            while (cursor2.hasNext()) {
                ScmSessionInfo si = cursor2.getNext();
                if (!si.getSessionId().equals(session.getSessionId())) {
                    ScmFactory.Session.deleteSession(session, si.getSessionId());
                }
            }
        }
        finally {
            if (cursor2 != null) {
                cursor2.close();
            }
        }
    }
}
