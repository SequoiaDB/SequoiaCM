package com.sequoiacm.directory;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class CreateDirParentNotExist extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "CreateDirParentNotExist";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

    }

    @Test
    public void test() throws ScmException {
        try {
            ScmFactory.Directory.createInstance(ws, "/notexist/" + dirName);
            Assert.fail("create dir success");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.DIR_NOT_FOUND) {
                throw e;
            }
        }
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
