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

public class TestScmBatchDetachFile extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmBatchDetachFile.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile file;
    private ScmId batchID;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        file = ScmFactory.File.createInstance(ws);
        file.setFileName(ScmTestTools.getClassName());
        file.save();

        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("TestBatchDetachFile");
        batchID = batch.save();
        batch.attachFile(file.getFileId());
    }

    @Test
    public void testDetach() throws ScmException {
        ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchID);
        List<ScmFile> files = batch.listFiles();
        Assert.assertEquals(files.size(), 1);

        logger.info("batch detach file");
        batch.detachFile(file.getFileId());

        try {
            // redo
            logger.info("batch detach file repeat");
            batch.detachFile(file.getFileId());
            Assert.fail("detach file which not belongs to the batch should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ScmError.FILE_NOT_IN_BATCH.getErrorCode(), e.getMessage());
        }

        // get batch again
        batch = ScmFactory.Batch.getInstance(ws, batchID);
        files = batch.listFiles();
        Assert.assertEquals(files.size(), 0);

        // delete file should be successful
        ScmFactory.File.deleteInstance(ws, file.getFileId(), true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, batchID);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
