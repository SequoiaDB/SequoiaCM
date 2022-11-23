package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ScmDataSpaceRecycler;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingCallback;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaHistoryDataTableNameAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScmSpaceRecycleTask extends ScmTaskBase {

    private static final Logger logger = LoggerFactory.getLogger(ScmSpaceRecycleTask.class);
    private static final String MONTH_BEFORE_SCOPE_SUFFIX = " month before";

    private BSONObject taskInfo;
    private String taskId;
    private ScmWorkspaceInfo wsInfo;
    private long maxExecTime = 0; // 0 means unlimited
    private int recycleMonthBeforeCount;
    private volatile boolean isRunning = true;
    private Date taskStartTime;
    private ScmDataSpaceRecycler scmDataSpaceRecycler;

    public ScmSpaceRecycleTask(ScmTaskManager mgr, BSONObject info) throws ScmServerException {
        super(mgr);
        taskInfo = info;
        taskId = (String) info.get(FieldName.Task.FIELD_ID);
        String wsName = (String) info.get(FieldName.Task.FIELD_WORKSPACE);
        wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        maxExecTime = BsonUtils.getLongOrElse(info, FieldName.Task.FIELD_MAX_EXEC_TIME,
                maxExecTime);
        BSONObject option = BsonUtils.getBSONChecked(info, FieldName.Task.FIELD_OPTION);
        recycleMonthBeforeCount = parseRecycleScope(
                BsonUtils.getStringChecked(option, FieldName.Task.FIELD_OPTION_RECYCLE_SCOPE));
        ScmContentModule contentModule = ScmContentModule.getInstance();
        taskInfoContext.setActualCount(-1);
        taskInfoContext.setEstimateCount(-1);
        try {
            Date beginningTime = getBeginningTime();
            Date endingTime = getEndingTime();
            scmDataSpaceRecycler = ScmDataOpFactoryAssit.getFactory().createScmDataSpaceRecycler(
                    queryAllTableNames(), beginningTime, endingTime, wsInfo.getName(),
                    contentModule.getLocalSiteInfo().getName(),
                    contentModule.getDataService());
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to create scmDataSpaceRecycler", e);
        }

    }

    private Date getBeginningTime() {
        // 目前没有开始时间的限制，因此返回一个最小值
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.getMinimum(Calendar.YEAR));
        calendar.set(Calendar.MONTH, calendar.getMinimum(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));
        // 0001-01-01 00:00:00.000
        return calendar.getTime();
    }

    private Date getEndingTime() {
        Calendar calendar = Calendar.getInstance();
        // recycleMonthBeforeCount 不能大于当前已经经历过的月数，否则会造成溢出，导致后面的计算不准确
        int reduceMonth = Math.min(calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH),
                recycleMonthBeforeCount);
        calendar.add(Calendar.MONTH, -reduceMonth);
        return calendar.getTime();
    }

    private int parseRecycleScope(String scopeStr) throws ScmInvalidArgumentException {
        try {
            if (scopeStr != null && scopeStr.endsWith(MONTH_BEFORE_SCOPE_SUFFIX)) {
                int num = Integer.parseInt(scopeStr.replace(MONTH_BEFORE_SCOPE_SUFFIX, "").trim());
                if (num < 0) {
                    throw new ScmInvalidArgumentException(FieldName.Task.FIELD_OPTION_RECYCLE_SCOPE
                            + " must be greater than or equal to 0");
                }
                return num;
            }
            else {
                throw new ScmInvalidArgumentException(FieldName.Task.FIELD_OPTION_RECYCLE_SCOPE
                        + "is invalid: scope=" + scopeStr);
            }
        }
        catch (ScmInvalidArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException(
                    FieldName.Task.FIELD_OPTION_RECYCLE_SCOPE + "is invalid: scope=" + scopeStr, e);
        }
    }

    @Override
    public String getName() {
        return "SCM_TASK_RECYCLE_SPACE";
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public int getTaskType() {
        return CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE;
    }

    @Override
    public BSONObject getTaskInfo() {
        return taskInfo;
    }

    @Override
    public void _stop() {
        isRunning = false;
    }

    @Override
    public void _runTask() {
        try {
            logger.info("running task:task=" + toString());
            taskStartTime = new Date();
            ScmSpaceRecyclingInfo recyclingInfo = scmDataSpaceRecycler.recycle(maxExecTime,
                    new ScmSpaceRecyclingCallback() {
                        @Override
                        public boolean shouldContinue() {
                            return isRunning;
                        }
                    });
            taskInfoContext.recordExtraInfo(recyclingInfo);

            updateProgress(recyclingInfo);

            // timeout
            if (recyclingInfo.isTimeout()) {
                logger.warn(
                        "task have been interrupted because of timeout:taskId={},startTime={},now={},maxExecTime={}ms",
                        getTaskId(), taskStartTime, new Date(), maxExecTime);
                abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_TIMEOUT,
                        "timeout", taskInfoContext.getSuccessCount(),
                        taskInfoContext.getFailedCount(), taskInfoContext.getProgress());
                return;
            }

            // canceled
            int flag = ScmTaskFile.getTaskRunningFlag(getTaskId());
            if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == flag) {
                logger.info("task have been canceled:taskId=" + getTaskId());
                updateTaskStopTimeAndAsyncRedo(getTaskId(), taskInfoContext.getSuccessCount(),
                        taskInfoContext.getFailedCount(), taskInfoContext.getProgress());
                return;
            }

            // finished
            logger.info("task have been finished:taskId=" + getTaskId());
            finishTaskAndAsyncRedo(getTaskId(), taskInfoContext.getSuccessCount(),
                    taskInfoContext.getFailedCount());
        }
        catch (Exception e) {
            logger.warn("do task failed:taskId=" + getTaskId(), e);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), taskInfoContext.getSuccessCount(),
                    taskInfoContext.getFailedCount(), taskInfoContext.getProgress());
        }

    }

    private void updateProgress(ScmSpaceRecyclingInfo recyclingInfo) throws ScmServerException {
        long actualCount = recyclingInfo.getSuccessCount() + recyclingInfo.getFailedCount();
        taskInfoContext.setActualCount(actualCount);
        taskInfoContext.setEstimateCount(actualCount);
        ScmContentModule.getInstance().getMetaService()
                .updateTaskActualAndEstimateCount(getTaskId(), actualCount, actualCount);
        taskInfoContext.setSuccessCount(recyclingInfo.getSuccessCount());
        taskInfoContext.setFailedCount(recyclingInfo.getFailedCount());
    }


    private List<String> queryAllTableNames() throws ScmServerException, ScmMetasourceException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        List<String> tableNames = new ArrayList<>();
        MetaHistoryDataTableNameAccessor accessor = contentModule.getMetaService().getMetaSource()
                .getDataTableNameHistoryAccessor();
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.DataTableNameHistory.WORKSPACE_NAME, wsInfo.getName());
        matcher.put(FieldName.DataTableNameHistory.SITE_NAME,
                contentModule.getLocalSiteInfo().getName());
        MetaCursor cursor = null;
        try {
            cursor = accessor.query(matcher,
                    new BasicBSONObject(FieldName.DataTableNameHistory.TABLE_NAME, null), null);
            while (cursor.hasNext()) {
                tableNames.add(BsonUtils.getStringChecked(cursor.getNext(),
                        FieldName.DataTableNameHistory.TABLE_NAME));
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tableNames;

    }

    @Override
    public String toString() {
        return "ScmSpaceRecycleTask{" + "taskInfo=" + taskInfo + ", taskId='" + taskId + '\'' + "} "
                + super.toString();
    }

    @Override
    protected long getEstimateCount() throws ScmServerException {
        return taskInfoContext.getEstimateCount();
    }

    @Override
    protected long getActualCount() throws ScmServerException {
        return taskInfoContext.getActualCount();
    }
}
