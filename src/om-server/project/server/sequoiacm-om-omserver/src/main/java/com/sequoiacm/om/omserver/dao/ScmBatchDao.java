package com.sequoiacm.om.omserver.dao;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;

public interface ScmBatchDao {
    public long countBatch(String wsName) throws ScmInternalException;

    public OmBatchDetail getBatchDetail(String wsName, String batchId) throws ScmInternalException;

    public List<OmBatchBasic> getBatchList(String wsName, BSONObject condition, long skip,
            int limit) throws ScmInternalException;
}
