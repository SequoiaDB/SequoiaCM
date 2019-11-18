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
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestScmBatchUpdate extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmBatchUpdate.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId batchId;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testUpdate() throws ScmException {
        final String oldBatchName = "TestScmBatchUpdate";
        final String newBatchName = "TestScmBatchUpdate-new";
        final String oldTagValue = "tagValue";
        final String newTagValue = "newTagValue";

        ScmTags notEmtpyTags = new ScmTags();
        notEmtpyTags.addTag(oldTagValue);

        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(oldBatchName);
        batch.setTags(notEmtpyTags);
        batchId = batch.save();

        batch = ScmFactory.Batch.getInstance(ws, batchId);
        logger.info("oldBatch: " + batch.toString());
        Assert.assertEquals(batch.getName(), oldBatchName);
        Assert.assertEquals(batch.getTags().toString(), notEmtpyTags.toString());

        /*
         * test set properties = null
         */
        try {
            batch.setClassProperties(null);
            Assert.fail("set batch properties=null should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        batch.setName(newBatchName);
        ScmTags newTags = new ScmTags();
        newTags.addTag(newTagValue);
        batch.setTags(newTags);

        batch = ScmFactory.Batch.getInstance(ws, batchId);
        logger.info("newBatch: " + batch.toString());
        Assert.assertEquals(batch.getName(), newBatchName);
        Assert.assertEquals(batch.getTags().contains(newTagValue), true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Batch.deleteInstance(ws, batchId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }

}
