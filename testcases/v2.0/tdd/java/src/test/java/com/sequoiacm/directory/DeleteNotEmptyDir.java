package com.sequoiacm.directory;

import org.junit.Assert;
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

public class DeleteNotEmptyDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testHaveDir() throws ScmException {
        String dirName = "DeleteNotEmptyDir_1";

        // clear
        ScmTestTools.clearDir(ws, "/" + dirName + "/" + dirName + "_sub1");
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmDirectory sub = dir.createSubdirectory(dirName + "_sub1");

        try {
            dir.delete();
            Assert.fail("delete success");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.DIR_NOT_EMPTY) {
                throw e;
            }
        }

        sub.delete();
        dir.delete();
    }

    @Test
    public void testHaveFile() throws ScmException {
        String dirName = "DeleteNotEmptyDir_2";

        // clear
        ScmTestTools.clearFile(ws, "/" + dirName + "/" + dirName + "_sub1");
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmFile f = ScmFactory.File.createInstance(ws);
        f.setFileName("subfile");
        f.setDirectory(dir);
        f.save();

        try {
            dir.delete();
            Assert.fail("delete success");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.DIR_NOT_EMPTY) {
                throw e;
            }
        }

        f.delete(true);
        dir.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
