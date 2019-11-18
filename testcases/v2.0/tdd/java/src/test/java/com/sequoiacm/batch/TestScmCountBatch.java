package com.sequoiacm.batch;

import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestScmCountBatch extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmCountBatch.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private final int batchNum = 5;
    private final String batchName = "TestScmCountBatch";

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testGetList() throws ScmException {
        for (int i = 0; i < batchNum; ++i) {
            ScmBatch batch = ScmFactory.Batch.createInstance(ws);
            batch.setName(batchName);
            ScmTags tags = new ScmTags();
            tags.addTag("tagVal_" + i);
            batch.setTags(tags);
            batch.save();
            logger.info(batch.toString());
        }

        long count = ScmFactory.Batch.countInstance(ws, new BasicBSONObject("name", batchName));
        Assert.assertEquals(count, batchNum);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws,
                    new BasicBSONObject("name", batchName));
            while (cursor.hasNext()) {
                ScmId batchId = cursor.getNext().getId();
                ScmFactory.Batch.deleteInstance(ws, batchId);
            }
            cursor.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
