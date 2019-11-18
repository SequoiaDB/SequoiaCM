package com.sequoiacm.batch;

import java.io.IOException;

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

public class TestScmBatchTags extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmBatchTags.class);
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
    public void testTags() throws ScmException, IOException {
        ScmBatch createdBatch = ScmFactory.Batch.createInstance(ws);
        createdBatch.setName(ScmTestTools.getClassName());

        // setTags
        ScmTags tags = new ScmTags();
        tags.addTag("tag-value1");
        tags.addTag("tag-value2");
        createdBatch.setTags(tags);
        batchId = createdBatch.save();
        checkTag(batchId, tags);

        tags = new ScmTags();
        // scmTag add tag = null
        try {
            tags.addTag(null);
            Assert.fail("tag's key cannot be null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        // scmTag add tag=''
        try {
            tags.addTag("");
            Assert.fail("tag's key cannot be empty");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        // batch add tag = null
        try {
            createdBatch.addTag(null);
            Assert.fail("tag's key cannot be null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        // batch add key=''
        try {
            createdBatch.addTag("");
            Assert.fail("tag's key cannot be empty");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        // set tags=null,set empty tags
        createdBatch.setTags(null);
        checkTag(batchId, new ScmTags());

        // batch add key contains '.'
        createdBatch.addTag("a.bc");
        // batch add key start with '$'
        createdBatch.addTag("$abc");
        ScmTags expectTag = new ScmTags();
        expectTag.addTag("a.bc");
        expectTag.addTag("$abc");
        checkTag(batchId, expectTag);

        // set(cover tags)
        ScmTags newTag = new ScmTags();
        newTag.addTag("aa");
        createdBatch.setTags(newTag);
        checkTag(batchId, newTag);

        // remove tag
        createdBatch.removeTag("aa");
        checkTag(batchId, new ScmTags());

    }

    private void checkTag(ScmId batchId, ScmTags expectTags) throws ScmException {
        ScmBatch savedBatch = ScmFactory.Batch.getInstance(ws, batchId);
        Assert.assertEquals(savedBatch.getTags().toString(), expectTags.toString());

    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(ws, batchId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        ScmTestTools.releaseSession(ss);
    }

}
