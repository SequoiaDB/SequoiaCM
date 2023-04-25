package com.sequoiacm.contentserver.service;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface ITaskService {
    BSONObject getTask(String taskId) throws ScmServerException;

    BSONObject getTaskDetail(String taskId) throws ScmServerException;

    /*
     * void getTaskList(PrintWriter writer, BSONObject condition) throws
     * ScmServerException;
     */
    MetaCursor getTaskList(BSONObject condition, BSONObject orderby, BSONObject selector, long skip,
            long limit) throws ScmServerException;

    String startTask(String serssionId, String userDetail, ScmUser user, String wsName,
            int taskType, int serverId, String targetSite, BSONObject options,
            Boolean isAsyncCountFile) throws ScmServerException;

    void stopTask(String sessionId, String userDetail, ScmUser user, String ws, String taskId)
            throws ScmServerException;

    void notifyTask(String taskId, int notifyType, boolean isAsyncCountFile)
            throws ScmServerException;

    long countTask(BSONObject condition) throws ScmServerException;
}
