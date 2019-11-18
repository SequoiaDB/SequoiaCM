package com.sequoiacm.batch;

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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestOprBeforeSave extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestOprBeforeSave.class);
    private ScmSession ss;
    private ScmWorkspace ws;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("TestOprBeforeSave");
        // batch.save();
        ScmId fileId = new ScmId("ffffffffffffffffffffffff");
        try {
            logger.info("attach file before save");
            batch.attachFile(fileId);
            Assert.fail("attach file should not be successful before save");
        } catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED);
        }

        try {
            logger.info("detach file before save");
            batch.detachFile(fileId);
            Assert.fail("detach file should not be successful");
        } catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED);
        }

        try {
            logger.info("list files before save");
            batch.listFiles();
            Assert.fail("list files should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED, e.getMessage());
        }

        try {
            logger.info("delete batch before save");
            batch.delete();
            Assert.fail("delete file not be successful");
        } catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNSUPPORTED, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(ss);
    }
}
