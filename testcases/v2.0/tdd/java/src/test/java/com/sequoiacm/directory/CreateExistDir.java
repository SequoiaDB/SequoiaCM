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

public class CreateExistDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testExistFile() throws ScmException {
        String dirName = CreateExistDir.class.getName() + "_1";

        // clear
        ScmTestTools.clearFile(ws, "/" + dirName);

        ScmFile f = ScmFactory.File.createInstance(ws);
        f.setFileName(dirName);
        f.save();

        try {
            ScmFactory.Directory.createInstance(ws, "/" + dirName);
            Assert.fail("create dir success");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.FILE_EXIST) {
                throw e;
            }
        }
        f.delete(true);
    }

    @Test
    public void testExistDir() throws ScmException {
        String dirName = CreateExistDir.class.getName() + "_2";

        // clear
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        try {
            ScmFactory.Directory.createInstance(ws, "/" + dirName);
            Assert.fail("create dir success");
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.DIR_EXIST) {
                throw e;
            }
        }
        dir.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
