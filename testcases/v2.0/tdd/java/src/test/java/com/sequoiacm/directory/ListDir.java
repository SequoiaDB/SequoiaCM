package com.sequoiacm.directory;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class ListDir extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "listDir";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        // clear
        ScmTestTools.clearDir(ws, "/" + dirName + "/subDir1/");
        ScmTestTools.clearDir(ws, "/" + dirName + "/subDir2/");
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory pdir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmDirectory sdir1 = ScmFactory.Directory.createInstance(ws, "/" + dirName + "/subDir1");
        ScmDirectory sdir2 = ScmFactory.Directory.createInstance(ws, "/" + dirName + "/subDir2");

        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.Directory.NAME).is("subDir1")
                .get();
        ScmCursor<ScmDirectory> c = pdir.listDirectories(cond);
        checkRes(c);

        c = ScmFactory.Directory.listInstance(ws, cond);
        checkRes(c);

        sdir1.delete();
        sdir2.delete();
        pdir.delete();
    }

    private void checkRes(ScmCursor<ScmDirectory> c) throws ScmException {
        try {
            if (c.hasNext()) {
                ScmDirectory d = c.getNext();
                Assert.assertEquals(d.getPath(), "/" + dirName + "/subDir1/");
                if (c.hasNext()) {
                    Assert.fail("unexpected dir:" + c.getNext());
                }
            }
            else {
                Assert.fail("no dir found");
            }
        }
        finally {
            c.close();
        }
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
