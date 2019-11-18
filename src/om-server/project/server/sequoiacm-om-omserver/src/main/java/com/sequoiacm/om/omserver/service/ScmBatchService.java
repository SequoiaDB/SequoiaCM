package com.sequoiacm.om.omserver.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmBatchService {
    public OmBatchDetail getBatch(ScmOmSession session, String wsName, String batchId)
            throws ScmInternalException, ScmOmServerException;

    public List<OmBatchBasic> getBatchList(ScmOmSession session, String wsName,
            BSONObject filter, long skip, int limit)
            throws ScmInternalException, ScmOmServerException;
}
