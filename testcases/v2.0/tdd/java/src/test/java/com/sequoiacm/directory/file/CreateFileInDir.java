package com.sequoiacm.directory.file;

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
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class CreateFileInDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = CreateFileInDir.class.getName();

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        // clear
        ScmTestTools.clearFile(ws, "/" + dirName + "/subfile");
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmFile f = ScmFactory.File.createInstance(ws);
        f.setDirectory(dir);
        f.setFileName("subfile");
        f.save();

        ScmFile f2 = ScmFactory.File.getInstanceByPath(ws, "/" + dirName + "/subfile");
        Assert.assertEquals(f2.getFileId().get(), f.getFileId().get());

        f2.delete(true);
        dir.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
