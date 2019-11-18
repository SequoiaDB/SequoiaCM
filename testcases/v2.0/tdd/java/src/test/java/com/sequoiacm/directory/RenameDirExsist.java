package com.sequoiacm.directory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class RenameDirExsist extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        String dirName = "RenameDir_1";

        // clear
        ScmTestTools.clearDir(ws, "/" + dirName);
        ScmTestTools.clearDir(ws, "/" + dirName + "_newName");

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmDirectory dir2 = ScmFactory.Directory.createInstance(ws, "/" + dirName + "_newName");
        try {
            dir.rename(dirName + "_newName");
            Assert.fail("reaname dir success!");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.DIR_EXIST) {
                throw e;
            }
        }
        dir.delete();
        dir2.delete();
    }

    @Test
    public void test2() throws ScmException {
        String dirName = "RenameDir_2";

        // clear
        ScmTestTools.clearDir(ws, "/" + dirName);
        ScmTestTools.clearFile(ws, "/" + dirName + "_newName");

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmFile f = ScmFactory.File.createInstance(ws);
        f.setFileName(dirName + "_newName");
        f.save();

        try {
            dir.rename(dirName + "_newName");
            Assert.fail("reaname dir success!");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.FILE_EXIST) {
                throw e;
            }
        }
        dir.delete();
        f.delete(true);
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
