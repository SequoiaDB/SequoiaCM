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

public class TestScmBatchCreate extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmBatchCreate.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    final String batchName = "TestCreateBatch";

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

    }

    @Test
    public void testCreate() throws ScmException {
        final int batchNum = 5;

        ScmTags tags = new ScmTags();
        tags.addTag("中文标签");

        for (int i = 0; i < batchNum; ++i) {
            ScmBatch batch = ScmFactory.Batch.createInstance(ws);
            batch.setName(batchName);
            batch.setTags(tags);
            batch.save();
            logger.info(batch.toString());
        }

        ScmCursor<ScmBatchInfo> cursor = null;
        try {
            cursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject("name", batchName));
            while (cursor.hasNext()) {
                ScmBatchInfo info = cursor.getNext();
                Assert.assertEquals(info.getName(), batchName);
                Assert.assertEquals(info.getFilesCount(), 0);

                ScmId batchId = info.getId();
                ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
                Assert.assertEquals(batch.getTags().toString(), tags.toString());
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @AfterClass
    private void tearDown() throws Exception {
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
