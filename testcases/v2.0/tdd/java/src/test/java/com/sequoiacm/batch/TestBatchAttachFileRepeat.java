package com.sequoiacm.batch;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestBatchAttachFileRepeat extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestBatchAttachFileRepeat.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile file;
    private ScmId batchId = null;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        file = ScmFactory.File.createInstance(ws);
        file.setFileName(ScmTestTools.getClassName());
        file.save();
    }

    @Test
    public void testAttachRepeat() throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("TestBatchAttachFileRepeat");
        batchId = batch.save();
        batch.attachFile(file.getFileId());

        // redo: attach file to batch
        try {
            logger.info("batch attach file repeat");
            batch.attachFile(file.getFileId());
            Assert.fail("attach a file again should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ScmError.FILE_IN_SPECIFIED_BATCH.getErrorCode(), e.getMessage());
        }

        batch = ScmFactory.Batch.getInstance(ws, batchId);
        List<ScmFile> files = batch.listFiles();
        Assert.assertEquals(files.size(), 1);
        Assert.assertEquals(files.get(0).getFileId().get(), file.getFileId().get());
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, batchId);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
