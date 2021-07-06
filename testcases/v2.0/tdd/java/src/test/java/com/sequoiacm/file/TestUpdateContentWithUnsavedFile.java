package com.sequoiacm.file;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

public class TestUpdateContentWithUnsavedFile extends ScmTestMultiCenterBase {

    private ScmSession session;
    private String filePath;
    private ScmBreakpointFile breakFile;
    private ScmWorkspace workspace;

    @BeforeClass
    public void setUp() throws ScmException {
        session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        this.filePath = workDir + File.separator + "TestUpdateContentWithUnsavedFile.data";
        ScmTestTools.createFile(filePath, " ", 1024);
        workspace = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session);
    }

    @Test
    public void testUpdateContentByFilePath() throws ScmException {
        ScmFile f = ScmFactory.File.createInstance(workspace);
        f.setFileName(UUID.randomUUID().toString());
        try {
            // update by file path
            f.updateContent(filePath);
            Assert.fail("updateContent in unsaved file should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED, e.getMessage());
        }
    }

    @Test
    public void testUpdateContentByBreakFile() throws ScmException {
        ScmFile f = ScmFactory.File.createInstance(workspace);
        f.setFileName(UUID.randomUUID().toString());
        try {
            // update by breakFile
            breakFile = ScmFactory.BreakpointFile.createInstance(workspace, "test",
                    ScmChecksumType.CRC32, 4 * 1024 * 1024);
            breakFile.upload(new File(filePath));
            f.updateContent(breakFile);
            Assert.fail("updateContent in unsaved file should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        if (breakFile != null) {
            ScmFactory.BreakpointFile.deleteInstance(workspace, breakFile.getFileName());
        }
        session.close();
    }
}
