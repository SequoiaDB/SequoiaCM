package com.sequoiacm.directory.file;

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
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class ListFile extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String dirName = "listFile";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        // clear
        ScmTestTools.clearFile(ws, "/" + dirName + "/subFile1/");
        ScmTestTools.clearFile(ws, "/" + dirName + "/subFile1/");
        ScmTestTools.clearDir(ws, "/" + dirName + "/subFile2/");
        ScmTestTools.clearDir(ws, "/" + dirName);

        ScmDirectory pdir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
        ScmFile subFile1 = ScmFactory.File.createInstance(ws);
        subFile1.setDirectory(pdir);
        subFile1.setFileName("subFile1");
        subFile1.save();

        ScmFile subFile2 = ScmFactory.File.createInstance(ws);
        subFile2.setDirectory(pdir);
        subFile2.setFileName("subFile2");
        subFile2.save();

        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.Directory.NAME).is("subFile1")
                .get();
        ScmCursor<ScmFileBasicInfo> c = pdir.listFiles(cond);

        try {
            if (c.hasNext()) {
                ScmFileBasicInfo f = c.getNext();
                Assert.assertEquals(f.getFileId().get(), subFile1.getFileId().get());
                if (c.hasNext()) {
                    Assert.fail("unexpected dir:" + c.getNext());
                }
            }
            else {
                Assert.fail("no file found");
            }
        }
        finally {
            c.close();
        }

        subFile1.delete(true);
        subFile2.delete(true);
        pdir.delete();
    }

    @AfterClass
    public void clear() {
        ss.close();
    }
}
