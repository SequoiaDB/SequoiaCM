package com.sequoiacm.file;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;

public class TestDeleteFile extends ScmTestMultiCenterBase {

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId fileId;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        createFile();
    }


    @Test
    public void testDeleteFile() throws ScmException {
        ScmFile scmFile = ScmFactory.File.getInstance(ws, fileId);
        Assert.assertFalse(scmFile.isDeleted());
        scmFile.delete(true);
        Assert.assertTrue(scmFile.isDeleted());
    }

    private void createFile() throws ScmException {
        ScmFile scmFile = ScmTestTools.createScmFile(ws, null, UUID.randomUUID().toString(), null,
                null);
        this.fileId = scmFile.getFileId();
    }


    @AfterClass
    public void tearDown() throws ScmException {
        if (ss != null) {
            ss.close();
        }
    }
}
