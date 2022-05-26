package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.sequoiadb.dataopertion.SdbDataReaderImpl;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

enum DoFileRes {
    SUCCESS,
    FAIL,
    SKIP,
    INTERRUPT;
}

public abstract class ScmTaskFile extends ScmTaskBase {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskFile.class);

    protected BSONObject taskInfo;
    private String taskId;
    private BSONObject taskMatcher;
    private int mainSiteId;
    private int localSiteId;
    private ScmWorkspaceInfo wsInfo;

    private Date preDate;
    private static int DATE_STEP = 20; // seconds
    private int preProgress = 0;
    private static int PROGRESS_STEP = 10;

    private long estimateCount = 0;
    private long actualCount = 0;

    private boolean running = true;
    private BSONObject actualMatcher;
    private int scope = CommonDefine.Scope.SCOPE_CURRENT;
    private long maxExecTime = 0; // 0 means unlimited

    private Date taskStartTime;

    // private int realTimeProgress = 0;

    public ScmTaskFile(ScmTaskManager mgr, BSONObject info) throws ScmServerException {
        super(mgr);
        try {
            taskInfo = info;
            taskId = (String) info.get(FieldName.Task.FIELD_ID);
            taskMatcher = (BSONObject) info.get(FieldName.Task.FIELD_CONTENT);
            String wsName = (String) info.get(FieldName.Task.FIELD_WORKSPACE);
            if (info.containsField(FieldName.Task.FIELD_SCOPE)) {
                scope = (int) info.get(FieldName.Task.FIELD_SCOPE);
            }
            ScmContentModule contentModule = ScmContentModule.getInstance();
            wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
            mainSiteId = contentModule.getMainSite();
            localSiteId = contentModule.getLocalSite();

            actualMatcher = buildActualMatcher();
            initTaskFileCount();
            maxExecTime = BsonUtils.getLongOrElse(info, FieldName.Task.FIELD_MAX_EXEC_TIME,
                    maxExecTime);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("initializing ScmTaskTransferFile failed", e);
        }
    }

    public int getMainSiteId() {
        return mainSiteId;
    }

    public int getLocalSiteId() {
        return localSiteId;
    }

    public ScmWorkspaceInfo getWorkspaceInfo() {
        return wsInfo;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void _stop() {
        running = false;
    }

    public BSONObject getTaskContent() {
        return taskMatcher;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(getTaskId()).append(",type=").append(getTaskType())
                .append(",content=").append(getTaskContent().toString());

        return sb.toString();
    }

    protected void closeReader(SdbDataReaderImpl reader) {
        FileCommonOperator.closeReader(reader);
    }

    public static int getTaskRunningFlag(String taskId) throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);

        BSONObject task = ScmContentModule.getInstance().getTaskInfo(matcher);
        if (null == task) {
            throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                    "task is inexistent:taskId=" + taskId);
        }

        return (int) task.get(FieldName.Task.FIELD_RUNNING_FLAG);
    }

    protected MetaCursor getCursor(ScmMetaService sms) throws ScmServerException {
        switch (scope) {
            case CommonDefine.Scope.SCOPE_CURRENT:
                return sms.queryCurrentFileIgnoreDeleteMarker(wsInfo, actualMatcher, null, null, 0, -1);
            case CommonDefine.Scope.SCOPE_ALL:
                return sms.queryAllFile(wsInfo, actualMatcher, null);
            case CommonDefine.Scope.SCOPE_HISTORY:
                return sms.queryHistoryFile(wsInfo.getMetaLocation(), wsInfo.getName(),
                        actualMatcher, null, null, 0, -1);
            default:
                throw new ScmInvalidArgumentException(
                        "runtask failed,unknow scope type:taskId=" + taskId + ",scope=" + scope);
        }
    }

    protected void closeCursor(MetaCursor cursor) {
        try {
            if (null != cursor) {
                cursor.close();
            }
        }
        catch (Exception e) {
            logger.warn("close task cursor failed:taskId=" + getTaskId(), e);
        }
    }

    protected void updateProgress(long successCount, long failedCount) {
        try {
            Date date = new Date();
            int seconds = ScmSystemUtils.getDuration(preDate, date);

            int progress = calculateProgress(successCount, failedCount);

            if (progress - preProgress >= PROGRESS_STEP || seconds > DATE_STEP) {
                ScmContentModule.getInstance().getMetaService().updateTaskProgress(taskId, progress,
                        successCount, failedCount);

                preProgress = progress;
                preDate = date;
            }
        }
        catch (Exception e) {
            logger.warn("updateProgress failed", e);
        }
    }

    private int calculateProgress(long successCount, long failedCount) {
        int progress = 0;
        if (actualCount > 0) {
            progress = (int) (100 * ((double) (successCount + failedCount) / actualCount));
        }

        if (progress >= 100) {
            progress = 99;
        }
        return progress;
    }

    private void initTaskFileCount() throws ScmServerException {
        ScmMetaService sms = ScmContentModule.getInstance().getMetaService();
        try {
            switch (scope) {
                case CommonDefine.Scope.SCOPE_CURRENT:
                    actualCount = sms.getCurrentFileCount(wsInfo, actualMatcher);
                    estimateCount = sms.getCurrentFileCount(wsInfo, taskMatcher);
                    break;
                case CommonDefine.Scope.SCOPE_ALL:
                    actualCount = sms.getAllFileCount(wsInfo.getMetaLocation(), wsInfo.getName(),
                            actualMatcher);
                    estimateCount = sms.getAllFileCount(wsInfo.getMetaLocation(), wsInfo.getName(),
                            taskMatcher);
                    break;
                case CommonDefine.Scope.SCOPE_HISTORY:
                    actualCount = sms.getHistoryFileCount(wsInfo.getMetaLocation(),
                            wsInfo.getName(), actualMatcher);
                    estimateCount = sms.getHistoryFileCount(wsInfo.getMetaLocation(),
                            wsInfo.getName(), taskMatcher);
                    break;
                default:
                    throw new ScmInvalidArgumentException("runtask failed,unknow scope type:taskId="
                            + taskId + ",scope=" + scope);
            }
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.INVALID_ARGUMENT) {
                throw e;
            }
            logger.warn("count file failed:taskId={},wsName={}", taskId, wsInfo.getName(), e);
        }
    }

    protected abstract DoFileRes doFile(BSONObject fileInfoNotInLock) throws ScmServerException;

    protected abstract BSONObject buildActualMatcher() throws ScmServerException;

    @Override
    public final void _runTask() {
        logger.debug("runing task:task=" + toString());
        // 1. get total count
        // totalCount = queryMatchingCount();
        logger.debug("task file info:taskId=" + getTaskId() + ",actual count=" + actualCount);

        ScmMetaService sms = null;
        MetaCursor cursor = null;
        try {
            sms = ScmContentModule.getInstance().getMetaService();
        }
        catch (ScmServerException e) {
            logger.warn("do task failed:taskId=" + getTaskId(), e);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), 0, 0, 0);
        }

        long successCount = 0;
        long failedCount = 0;
        try {
            // for update progress
            preDate = new Date();
            preProgress = 0;

            // for interrupt task if exceed maxExecTime
            taskStartTime = preDate;

            cursor = getCursor(sms);

            while (running && cursor.hasNext()) {
                BSONObject fileInfoNotInLock = cursor.getNext();
                String fileId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_ID);
                ScmLockPath lockPath = ScmLockPathFactory
                        .createFileLockPath(getWorkspaceInfo().getName(), fileId);
                DoFileRes res;
                ScmLock fileReadLock = ScmLockManager.getInstance().acquiresReadLock(lockPath);
                try {
                    res = doFile(fileInfoNotInLock);
                }
                finally {
                    fileReadLock.unlock();
                }

                if (res.equals(DoFileRes.SUCCESS)) {
                    successCount++;
                }
                else if (res.equals(DoFileRes.FAIL)) {
                    failedCount++;
                }

                // if (res == SKIP || res == INTERRUPT){
                // there is no special operation,ignore it
                // }

                updateProgress(successCount, failedCount);

                int flag = getTaskRunningFlag(getTaskId());
                if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == flag) {
                    logger.info("task have been canceled:taskId=" + getTaskId());
                    updateTaskStopTimeAndAsyncRedo(getTaskId(), successCount, failedCount,
                            calculateProgress(successCount, failedCount));
                    return;
                }

                if (maxExecTime > 0) {
                    Date now = new Date();
                    if (now.getTime() - taskStartTime.getTime() > maxExecTime) {
                        logger.warn(
                                "task have been interrupted because of timeout:taskId={},startTime={},now={},maxExecTime={}ms",
                                getTaskId(), taskStartTime, now, maxExecTime);
                        abortTaskAndAsyncRedo(getTaskId(),
                                CommonDefine.TaskRunningFlag.SCM_TASK_TIMEOUT, "timeout",
                                successCount, failedCount,
                                calculateProgress(successCount, failedCount));
                        return;
                    }
                }
            }

            if (!running) {
                logger.info("task have been canceled:taskId=" + getTaskId());
                updateTaskStopTimeAndAsyncRedo(getTaskId(), successCount, failedCount,
                        calculateProgress(successCount, failedCount));
            }
            else {
                logger.info("task have finished:taskId=" + taskId);
                finishTaskAndAsyncRedo(getTaskId(), successCount, failedCount);
            }
        }
        catch (Exception e) {
            logger.warn("do task failed:taskId=" + getTaskId(), e);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), successCount, failedCount,
                    calculateProgress(successCount, failedCount));
        }
        finally {
            closeCursor(cursor);
        }
    }

    @Override
    protected long getEstimateCount() {
        return estimateCount;
    }

    @Override
    protected long getActualCount() {
        return actualCount;
    }

    public Date getTaskStartTime() {
        return taskStartTime;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }
}
