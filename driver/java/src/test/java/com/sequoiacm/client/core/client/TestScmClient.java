package com.sequoiacm.client.core.client;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmClient extends ScmTestBase {

    @Test
    public void testClient() throws ScmException {
        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, user, password));
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        if (null != ws) {
            System.out.println("name:" + ws.getName());
        }

        session.close();

        Assert.assertNotNull(ws);
    }
}
