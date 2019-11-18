package com.sequoiacm.batch;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmBatchPageList extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private String batchName1 = "listBatchTest1";
    private String batchName2 = "listBatchTest2";
    private String batchName3 = "listBatchTest3";
    private ScmId id1;
    private ScmId id2;
    private ScmId id3;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void test() throws Exception {
        id1 = createBatch(batchName1);
        id2 = createBatch(batchName2);
        id3 = createBatch(batchName3);

        BSONObject emptyCondition = new BasicBSONObject();
        BSONObject ascOrder = new BasicBSONObject(ScmAttributeName.Batch.CREATE_TIME, 1);
        BSONObject descOrder = new BasicBSONObject(ScmAttributeName.Batch.CREATE_TIME, -1);

        // orderBy: asc desc
        qeuryAndCheckOrderBy(ws, emptyCondition, ascOrder, true);
        qeuryAndCheckOrderBy(ws, emptyCondition, descOrder, false);

        // limit
        boolean[] check1 = { true, true, false };
        qeuryAndCheckPage(ws, emptyCondition, descOrder, 0, 2, check1);
        // skip
        boolean[] check2 = { false, true, true };
        qeuryAndCheckPage(ws, emptyCondition, descOrder, 1, -1, check2);
        // page: skip limit
        boolean[] check3 = { false, true, false };
        qeuryAndCheckPage(ws, emptyCondition, descOrder, 1, 1, check3);

        // condition
        BSONObject condition = new BasicBSONObject(ScmAttributeName.Batch.NAME, batchName2);
        qeuryAndCheckCondition(ws, condition, null, 0, -1, batchName2);

    }

    @AfterClass
    public void cleanUp() throws ScmException {
        try {
            deleteBatch(id1);
            deleteBatch(id2);
            deleteBatch(id3);
        }
        finally {
            ss.close();
        }
    }

    private void deleteBatch(ScmId id) throws ScmException {
        ScmFactory.Batch.deleteInstance(ws, id);
    }

    private ScmId createBatch(String batchName) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(batchName);
        ScmTags tags = new ScmTags();
        tags.addTag("tagVal");
        batch.setTags(tags);
        return batch.save();
    }

    // page
    private void qeuryAndCheckPage(ScmWorkspace ws, BSONObject condition, BSONObject order,
            long skip, int limit, boolean[] check) throws Exception {
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, condition, order, skip,
                limit);
        Set<String> batchName = new HashSet<String>();
        while (cursor.hasNext()) {
            ScmBatchInfo currentItem = cursor.getNext();
            batchName.add(currentItem.getName());
        }
        assertEquals(batchName.contains(batchName3), check[0]);
        assertEquals(batchName.contains(batchName2), check[1]);
        assertEquals(batchName.contains(batchName1), check[2]);
        cursor.close();
    }

    // orderby
    private void qeuryAndCheckOrderBy(ScmWorkspace ws, BSONObject condition, BSONObject order,
            boolean isAsc) throws Exception {
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, condition, order, 0, -1);
        ScmBatchInfo currentItem = null;
        ScmBatchInfo nextItem = null;
        if (cursor.hasNext()) {
            currentItem = cursor.getNext();
        }
        while (cursor.hasNext()) {
            nextItem = cursor.getNext();
            assertEquals(nextItem.getCreateTime().getTime() > currentItem.getCreateTime().getTime(),
                    isAsc);
            currentItem = nextItem;
        }
        cursor.close();
    }

    private void qeuryAndCheckCondition(ScmWorkspace ws, BSONObject condition, BSONObject order,
            long skip, int limit, String batchName) throws ScmException {
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, condition, order, skip,
                limit);
        if (cursor.hasNext()) {
            ScmBatchInfo currentItem = cursor.getNext();
            assertEquals(currentItem.getName(), batchName);
        }
        assertEquals(cursor.hasNext(), false);
    }

}
