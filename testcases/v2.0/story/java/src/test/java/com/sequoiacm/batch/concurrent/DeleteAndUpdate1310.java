package com.sequoiacm.batch.concurrent;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1310: 并发删除批次和更新属性
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DeleteAndUpdate1310 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private final String batchName = "batch1310";
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = false)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);

        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        batchId = batch.save();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        UpdateThread updateThrd = new UpdateThread();
        DeleteThread deleteThrd = new DeleteThread();
        updateThrd.start();
        deleteThrd.start();
        Assert.assertTrue(updateThrd.isSuccess(), updateThrd.getErrorMsg());
        Assert.assertTrue(deleteThrd.isSuccess(), deleteThrd.getErrorMsg());

        ScmCursor<ScmBatchInfo> batchCursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject("id", batchId.get()));
        Assert.assertFalse(batchCursor.hasNext());
        batchCursor.close();
    }

    @AfterClass(alwaysRun = false)
    private void tearDown() throws Exception {
        if (session != null) {
            session.close();
        }
    }

    private class DeleteThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            // TODO: using global ws is rude. remember to create new session when free.
            Thread.sleep(50);
            ScmFactory.Batch.deleteInstance(ws, batchId);
        }
    }

    private class UpdateThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            try {
                ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
                ScmTags props = new ScmTags();
                for (int i = 0; i < 100; ++i) {
                    props.addTag( "value" + i);
                    batch.setTags(props);
                    batch.setTags(props);
                }
            } catch (ScmException e) {
                if (e.getError() != ScmError.BATCH_NOT_FOUND) {
                    throw e;
                }
            }
        }
    }
}