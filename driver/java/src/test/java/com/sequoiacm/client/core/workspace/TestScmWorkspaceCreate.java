package com.sequoiacm.client.core.workspace;

import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmWorkspaceCreate extends ScmTestBase {

    @Test
    public void testCreate() throws ScmException {
        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, user, password));
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        if (null != ws) {
            System.out.println("create workspace success.");
        }
        else {
            System.out.println("create workspace failed.");
        }

        session.close();
    }
}
