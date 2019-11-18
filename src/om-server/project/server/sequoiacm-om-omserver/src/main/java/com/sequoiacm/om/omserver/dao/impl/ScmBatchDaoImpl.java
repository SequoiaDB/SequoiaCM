package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmBatchDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;
import com.sequoiacm.om.omserver.session.ScmOmSessionImpl;

public class ScmBatchDaoImpl implements ScmBatchDao {

    private ScmOmSessionImpl session;

    public ScmBatchDaoImpl(ScmOmSessionImpl session) {
        this.session = session;
    }

    @Override
    public long countBatch(String wsName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            return ScmFactory.Batch.countInstance(ws, new BasicBSONObject());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count batch, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public OmBatchDetail getBatchDetail(String wsName, String batchId)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmBatch batch = ScmFactory.Batch.getInstance(ws, new ScmId(batchId));
            return new OmBatchDetail(batch);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to get batch, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public List<OmBatchBasic> getBatchList(String wsName, BSONObject condition, long skip,
            int limit) throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<ScmBatchInfo> cursor = null;

        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            List<OmBatchBasic> batchs = new ArrayList<>();
            cursor = ScmFactory.Batch.listInstance(ws, condition,
                    ScmQueryBuilder.start(ScmAttributeName.Batch.ID).is(1).get(), skip, limit);
            while (cursor.hasNext()) {
                ScmBatchInfo batch = cursor.getNext();
                batchs.add(new OmBatchBasic(batch));
            }
            return batchs;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get batch list, " + e.getMessage(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
