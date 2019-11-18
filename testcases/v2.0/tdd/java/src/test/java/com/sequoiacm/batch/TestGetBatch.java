package com.sequoiacm.batch;

import java.util.ArrayList;
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
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestGetBatch extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestGetBatch.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId batchId;
    private final String batchName = "TestGetBatch";
    private final int fileNum = 5;
    private List<ScmId> fileIdList = new ArrayList<>(fileNum);

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        for (int i = 0; i < fileNum; ++i) {
            logger.info("create file: " + ScmTestTools.getClassName() + i);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(ScmTestTools.getClassName() + i);
            file.setTitle(batchName);
            ScmId fileId = file.save();
            fileIdList.add(fileId);
        }
    }

    @Test
    public void testGet() throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        ScmTags tags = new ScmTags();
        tags.addTag("props");
        batch.setTags(tags);
        batchId = batch.save();
        for (ScmId fileId : fileIdList) {
            batch.attachFile(fileId);
        }

        logger.info(batch.toString());

        batch = ScmFactory.Batch.getInstance(ws, batchId);
        Assert.assertEquals(batch.getName(), batchName);
        Assert.assertEquals(batch.getId().get(), batchId.get());
        Assert.assertEquals(batch.getTags().toString(), tags.toString());
        List<ScmFile> files = batch.listFiles();
        Assert.assertEquals(files.size(), fileNum);
        for (ScmFile file : files) {
            Assert.assertEquals(file.getTitle(), batchName);
        }

        ScmId inexistentId = new ScmId("ffffffffffffffffffffffff");
        try {
            ScmFactory.Batch.getInstance(ws, inexistentId);
            Assert.fail("get not exist batch should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.BATCH_NOT_FOUND);
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, batchId);
            for (ScmId fileId : fileIdList) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
