package com.sequoiacm.metasource;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;

import java.util.Date;

public interface MetaTaskAccessor extends MetaAccessor {
    public void delete(String taskId) throws ScmMetasourceException;

    public void abortAllTask(int serverId, Date stopTime) throws ScmMetasourceException;

    public void abort(String taskId, int flag, String detail, Date stopTime, int progress,
            long successCount, long failedCount) throws ScmMetasourceException;

    public void cancel(String taskId, String detail) throws ScmMetasourceException;

    public void finish(String taskId, Date stopTime, long successCount,
            long failedCount) throws ScmMetasourceException;

    public void setAllStopTime(int serverId, Date stopTime) throws ScmMetasourceException;

    public boolean checkAndStartTask(String taskId, Date startTime, long estimateCount,
            long actualCount) throws ScmMetasourceException;

    public void updateTaskFileCount(String taskId, long estimateCount, long actualCount)
            throws ScmMetasourceException;

    public void updateTaskStartExecuteTime(String taskId, long startExecuteTime)
            throws ScmMetasourceException;

    public void updateProgress(String taskId, int progress, long successCount, long failedCount)
            throws ScmMetasourceException;

    public void updateStopTimeIfEmpty(String taskId, Date stopTime, int progress, long successCount,
            long failedCount) throws ScmMetasourceException;

    void updateExtraInfo(String taskId, BSONObject extraInfo) throws SdbMetasourceException;

    void updateActualAndEstimateCount(String taskId, long actualCount, long estimateCount)
            throws SdbMetasourceException;
}
