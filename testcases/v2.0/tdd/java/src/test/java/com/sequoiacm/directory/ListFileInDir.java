package com.sequoiacm.directory;

import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class ListFileInDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "ListFileInDir";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        // clear
        clearFileAndDir();

        ScmDirectory pdir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmFile f = ScmFactory.File.createInstance(ws);
        f.setFileName("3");
        f.setDirectory(pdir);
        f.save();

        f = ScmFactory.File.createInstance(ws);
        f.setFileName("2");
        f.setDirectory(pdir);
        f.save();

        f = ScmFactory.File.createInstance(ws);
        f.setFileName("4");
        f.setDirectory(pdir);
        f.save();

        f = ScmFactory.File.createInstance(ws);
        f.setFileName("1");
        f.setDirectory(pdir);
        f.save();

        ScmCursor<ScmFileBasicInfo> cursor = pdir.listFiles(null, 1, 2,
                new BasicBSONObject("name", 1));
        Assert.assertEquals(cursor.getNext().getFileName(), "2");
        Assert.assertEquals(cursor.getNext().getFileName(), "3");

        cursor.close();
        clearFileAndDir();
    }

    private void clearFileAndDir() throws ScmException {
        ScmTestTools.clearFile(ws, "/" + dirName + "/1");
        ScmTestTools.clearFile(ws, "/" + dirName + "/2");
        ScmTestTools.clearFile(ws, "/" + dirName + "/3");
        ScmTestTools.clearFile(ws, "/" + dirName + "/4");
        ScmTestTools.clearDir(ws, "/" + dirName);
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
