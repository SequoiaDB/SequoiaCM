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

public class MoveDir extends ScmTestMultiCenterBase {
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
        //clear
        ScmTestTools.clearDir(ws, "/" + dirName + "_2/subDir/");
        ScmTestTools.clearDir(ws, "/" + dirName + "_1/subDir/");
        ScmTestTools.clearDir(ws, "/" + dirName + "_2");
        ScmTestTools.clearDir(ws, "/" + dirName + "_1");

        ScmDirectory pdir1 = ScmFactory.Directory.createInstance(ws, "/" + dirName + "_1");
        ScmDirectory pdir2 = ScmFactory.Directory.createInstance(ws, "/" + dirName + "_2");

        ScmDirectory subdir = pdir1.createSubdirectory("subDir");

        subdir.move(pdir2);

        Assert.assertEquals(subdir.getPath(), "/" + dirName + "_2/subDir/");

        subdir.delete();
        pdir1.delete();
        pdir2.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
