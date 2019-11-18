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

public class CreateDeleteDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "testcreateDir";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        deleteDir();
    }

    @Test
    public void test() throws ScmException {
        ScmDirectory dir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmDirectory sub = dir.createSubdirectory(dirName + "_sub1");
        ScmDirectory dir_1 = ScmFactory.Directory.getInstance(ws, "/" + dirName);
        ScmDirectory sub_1 = ScmFactory.Directory.getInstance(ws,
                "/" + dirName + "/" + dirName + "_sub1");
        Assert.assertEquals(dir.getPath(), dir_1.getPath());
        Assert.assertEquals(sub_1.getPath(), sub.getPath());

        sub.delete();
        ScmFactory.Directory.deleteInstance(ws, "/" + dirName);

        try {
            ScmFactory.Directory.getInstance(ws, "/" + dirName);
            Assert.fail("delete failed");
        }
        catch (ScmException e) {
            if (e.getErrorCode() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.getInstance(ws, "/" + dirName + "/" + dirName + "_sub1");
            Assert.fail("delete failed");
        }
        catch (ScmException e) {
            if (e.getErrorCode() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }

    }

    private void deleteDir() throws ScmException {
        try {
            ScmDirectory sub = ScmFactory.Directory.getInstance(ws,
                    "/" + dirName + "/" + dirName + "_sub1");
            sub.delete();

        }
        catch (ScmException e) {
            if (e.getErrorCode() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance(ws, "/" + dirName);
            dir.delete();
        }
        catch (ScmException e) {
            if (e.getErrorCode() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
