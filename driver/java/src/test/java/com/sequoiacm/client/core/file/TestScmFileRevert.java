package com.sequoiacm.client.core.file;

import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;

public class TestScmFileRevert extends ScmTestBase {

    @Test
    public static void testRevert() throws ScmException {
        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, user, password));
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

        ScmFile file = ScmFactory.File.getInstance(ws, new ScmId("fileId"));

        int preVersion = 1;
        //        file.revert(preVersion);

        session.close();
    }
}
