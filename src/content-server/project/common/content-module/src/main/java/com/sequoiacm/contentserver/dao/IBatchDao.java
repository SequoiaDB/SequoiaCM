package com.sequoiacm.contentserver.dao;

import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;

public interface IBatchDao {

    String insert(ScmWorkspaceInfo wsInfo, BSONObject batchInfo, String userName)
            throws ScmServerException;

    BSONObject queryById(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth)
            throws ScmServerException;

    MetaCursor query(ScmWorkspaceInfo wsInfo, BSONObject matcher, BSONObject orderBy, long skip,
            long limit) throws ScmServerException;

    boolean updateById(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth,
            BSONObject updator, String user) throws ScmServerException;

}