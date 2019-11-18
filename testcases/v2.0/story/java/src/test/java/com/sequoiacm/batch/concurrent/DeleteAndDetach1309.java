package com.sequoiacm.batch.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1309: 并发删除批次和批次中解除文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DeleteAndDetach1309 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private final String batchName = "batch1309";
    private final int fileNum = 30;
    private List<ScmId> fileIdList = new ArrayList<>(fileNum);
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);

        for (int i = 0; i < fileNum; ++i) {
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName("file1309_" + i);
            file.setTitle(batchName);
            ScmId fileId = file.save();
            fileIdList.add(fileId);
        }

        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        batchId = batch.save();
        for (ScmId fileId : fileIdList) {
            batch.attachFile(fileId);
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        DetachThread detachThrd = new DetachThread();
        DeleteThread deleteThrd = new DeleteThread();
        detachThrd.start();
        deleteThrd.start();
        Assert.assertTrue(detachThrd.isSuccess(), detachThrd.getErrorMsg());
        Assert.assertTrue(deleteThrd.isSuccess(), deleteThrd.getErrorMsg());

        ScmCursor<ScmBatchInfo> batchCursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject("id", batchId.get()));
        Assert.assertFalse(batchCursor.hasNext());
        batchCursor.close();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScmType.ScopeType.SCOPE_CURRENT,
                        new BasicBSONObject("title", batchName));
                while (cursor.hasNext()) {
                    ScmFileBasicInfo info = cursor.getNext();
                    ScmFactory.File.deleteInstance(ws, info.getFileId(), true);
                }
                cursor.close();
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private class DeleteThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            Thread.sleep(50);
            ScmFactory.Batch.deleteInstance(ws, batchId);
        }
    }

    private class DetachThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            try {
                ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
                for (ScmId fileId : fileIdList) {
                    batch.detachFile(fileId);
                }
            } catch (ScmException e) {
                if (e.getError() != ScmError.BATCH_NOT_FOUND) {
                    throw e;
                }
            }
        }
    }
}