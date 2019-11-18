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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestScmBatchDelete extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmBatchDelete.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private final int fileNum = 5;
    private List<ScmId> fileIdList = new ArrayList<>(fileNum);

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testDeleteInstance1() throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("TestDeleteBatch1");
        ScmId batchId = batch.save();
        for (int i = 0; i < fileNum; ++i) {
            logger.info("create file: " + ScmTestTools.getClassName() + i);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(ScmTestTools.getClassName() + i);
            ScmId fileId = file.save();
            fileIdList.add(fileId);

            batch.attachFile(fileId);
        }
        batch.delete();

        try {
            logger.info("get not exist batch");
            ScmFactory.Batch.getInstance(ws, batchId);
            Assert.fail("get not exist batch should not be successful");
        } catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(), ScmError.BATCH_NOT_FOUND.getErrorCode(), e.getMessage());
        }

        for (ScmId fileId : fileIdList) {
            try {
                logger.info("get not exist file: " + fileId.get());
                ScmFactory.File.getInstance(ws, fileId);
                Assert.fail("file should not exist");
            } catch (ScmException e) {
                Assert.assertEquals(e.getError(), ScmError.FILE_NOT_FOUND, e.getMessage());
            }
        }
        fileIdList.clear();
    }

    @Test(dependsOnMethods = {"testDeleteInstance1"})
    public void testDeleteInstance2() throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("TestDeleteBatch2");
        ScmId batchId = batch.save();
        for (int i = 0; i < fileNum; ++i) {
            logger.info("create file: " + ScmTestTools.getClassName() + i);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(ScmTestTools.getClassName() + i);
            ScmId fileId = file.save();
            fileIdList.add(fileId);

            batch.attachFile(fileId);
        }
        ScmFactory.Batch.deleteInstance(ws, batchId);

        try {
            logger.info("get not exist batch");
            ScmFactory.Batch.getInstance(ws, batchId);
            Assert.fail("get not exist batch should not be successful");
        } catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.BATCH_NOT_FOUND, e.getMessage());
        }

        for (ScmId fileId : fileIdList) {
            try {
                logger.info("get not exist file: " + fileId.get());
                ScmFactory.File.getInstance(ws, fileId);
                Assert.fail("file should not exist");
            } catch (ScmException e) {
                Assert.assertEquals(e.getError(), ScmError.FILE_NOT_FOUND, e.getMessage());
            }
        }

    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(ss);
    }
}
