package com.sequoiacm.directory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class RenameDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "RenameDir";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        // clear
        ScmTestTools.clearDir(ws, "/" + dirName);
        ScmTestTools.clearDir(ws, "/" + dirName + "_newName");

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        dir.rename(dirName + "_newName");
        Assert.assertEquals(dir.getName(), dirName + "_newName");

        ScmDirectory dir_newName = ScmFactory.Directory.getInstance(ws, "/" + dirName + "_newName");
        Assert.assertEquals(dir_newName.getName(), dirName + "_newName");
        dir.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
