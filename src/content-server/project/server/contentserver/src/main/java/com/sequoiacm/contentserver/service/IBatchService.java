package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface IBatchService {

    BSONObject getBatchInfo(String workspaceName, String batchId, boolean isDetail)
            throws ScmServerException;

    /*
     * void getList(PrintWriter writer, String workspaceName, BSONObject
     * matcher) throws ScmServerException;
     */

    MetaCursor getList(String workspaceName, BSONObject matcher, BSONObject orderBy, long skip,
            long limit) throws ScmServerException;

    void delete(String sessionId, String userDetail, String userName, String workspaceName,
            String batchId) throws ScmServerException;

    String create(String user, String workspaceName, BSONObject batchInfo)
            throws ScmServerException;

    void update(String user, String workspaceName, String batchId, BSONObject updator)
            throws ScmServerException;

    void attachFile(String user, String workspaceName, String batchId, String fileId)
            throws ScmServerException;

    void detachFile(String user, String workspaceName, String batchId, String fileId)
            throws ScmServerException;

    long countBatch(String wsName, BSONObject condition) throws ScmServerException;
}
