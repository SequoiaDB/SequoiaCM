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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class MoveDirToSubdir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "moveDir";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        // clear
        ScmTestTools.clearDir(ws, "/" + dirName + "/subDir/");
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory pdir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmDirectory subDir = ScmFactory.Directory.createInstance(ws, "/" + dirName + "/subDir/");

        try {
            pdir.move(subDir);
            Assert.fail("move file success!");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.DIR_MOVE_TO_SUBDIR) {
                throw e;
            }
        }
        Assert.assertEquals(pdir.getParentDirectory().getName(), "/");

        subDir.delete();
        pdir.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
