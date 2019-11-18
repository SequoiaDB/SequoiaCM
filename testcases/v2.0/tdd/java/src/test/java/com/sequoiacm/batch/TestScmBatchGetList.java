package com.sequoiacm.batch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestScmBatchGetList extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmBatchGetList.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private final int batchNum = 5;
    private final String batchName = "TestGetBatchList";

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

        int total = 0;
        BSONObject filter = ScmQueryBuilder.start(ScmAttributeName.Batch.NAME).is(batchName).get();
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, filter);
        while (cursor.hasNext()) {
            ScmBatchInfo info = cursor.getNext();
            Assert.assertEquals(batchName, info.getName());
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, batchNum, "wrong batch num");

        total = 0;
        ScmTags tags = new ScmTags();
        tags.addTag("tagVal_0");
        cursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject("tags", tags.toSet()));
        while (cursor.hasNext()) {
            ScmBatchInfo info = cursor.getNext();
            ScmId batchId = info.getId();
            ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
            Assert.assertEquals(batch.getTags().toSet().containsAll(tags.toSet()), true);
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, 1, "wrong batch num");

        total = 0;
        BSONObject illegalMatchCond = (BSONObject) JSON.parse("{ name: { $dd: '???' } }");
        try {
            cursor = ScmFactory.Batch.listInstance(ws, illegalMatchCond);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METASOURCE_ERROR, e.getMessage());
        }

        while (cursor.hasNext()) {
            cursor.getNext();
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, 0, "wrong batch num");
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
