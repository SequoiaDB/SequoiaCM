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

public class TestTwoBatchAttachSameFile extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestTwoBatchAttachSameFile.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile file;
    private ScmId batchIdA;
    private ScmId batchIdB;

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
    public void testTwoBatchAttachSameFile() throws ScmException {
        ScmBatch batchA = ScmFactory.Batch.createInstance(ws);
        batchA.setName("TestTwoBatchAttachSameFile-A");
        batchIdA = batchA.save();
        logger.info("batchA attach file: " + file.getFileId());
        batchA.attachFile(file.getFileId());

        ScmBatch batchB = ScmFactory.Batch.createInstance(ws);
        batchB.setName("TestTwoBatchAttachSameFile-B");
        batchIdB = batchB.save();

        // attach file which already in another batch
        try {
            logger.info("batchB attach file too");
            batchB.attachFile(file.getFileId());
            Assert.fail("attach a file which already in another batch should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ScmError.FILE_IN_ANOTHER_BATCH.getErrorCode(), e.getMessage());
        }

        List<ScmFile> files = batchA.listFiles();
        Assert.assertEquals(files.size(), 1);
        Assert.assertEquals(files.get(0).getFileId(), file.getFileId());

        files = batchB.listFiles();
        Assert.assertEquals(files.size(), 0);
    }


    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, batchIdA);
            ScmFactory.Batch.deleteInstance(ws, batchIdB);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
