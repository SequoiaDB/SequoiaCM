package com.sequoiacm.contentserver.service;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface IBatchService {

    BSONObject getBatchInfo(ScmUser user, String workspaceName, String batchId, boolean isDetail)
            throws ScmServerException;

    BSONObject getBatchInfo(String workspaceName, String batchId, boolean isDetail)
            throws ScmServerException;
    /*
     * void getList(PrintWriter writer, String workspaceName, BSONObject matcher)
     * throws ScmServerException;
     */

    MetaCursor getList(ScmUser user, String workspaceName, BSONObject matcher, BSONObject orderBy,
            long skip, long limit) throws ScmServerException;

    void delete(String sessionId, String userDetail, ScmUser user, String workspaceName,
            String batchId) throws ScmServerException;

    String create(ScmUser user, String workspaceName, BSONObject batchInfo)
            throws ScmServerException;

    void update(ScmUser user, String workspaceName, String batchId, BSONObject updator)
            throws ScmServerException;

    void attachFile(ScmUser user, String workspaceName, String batchId, String fileId)
            throws ScmServerException;

    void detachFile(ScmUser user, String workspaceName, String batchId, String fileId)
            throws ScmServerException;

    long countBatch(ScmUser user, String wsName, BSONObject condition) throws ScmServerException;
}
