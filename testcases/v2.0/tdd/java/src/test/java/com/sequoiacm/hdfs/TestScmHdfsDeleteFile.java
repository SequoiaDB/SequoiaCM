package com.sequoiacm.hdfs;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmHdfsDeleteFile extends ScmTestMultiCenterBase {

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile file;
    private ScmId scmId = null;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer3().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        file = ScmFactory.File.createInstance(ws);
        file.setContent(getDataDirectory() + File.separator + "test.txt");
        file.setFileName("test_hdfs_delete_file");
        scmId = file.save();
    }

    @Test
    public void testDelete() throws ScmException {

        file.delete(true);
        try {
            // read the deleted file
            ScmFile deletedFile = ScmFactory.File.getInstance(ws, scmId);

            Assert.fail("getInstance success:" + deletedFile);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ScmError.FILE_NOT_FOUND.getErrorCode(), e.getMessage());
        }

    }

    @Test
    public void testDeleteFileIsNotExist() throws ScmException {

        file.delete(true);
        try {

            file.delete(true);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ScmError.FILE_NOT_FOUND.getErrorCode(), e.getMessage());
        }

    }

    @AfterClass
    public void tearDown() throws ScmException {
        ss.close();
    }

}
