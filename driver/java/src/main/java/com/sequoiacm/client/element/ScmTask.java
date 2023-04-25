package com.sequoiacm.client.element;

import java.util.Date;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.common.CommonDefine;
import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;

/**
 * An abstract representation of scm task.
 */
public class ScmTask {
    private ScmTaskBasicInfo basic;
    private BSONObject content;
    private int serverId;
    private int progress;
    private int scope;
    private Date stopTime;
    private long estimateCount;
    private long actualCount;
    private long successCount;
    private long failCount;
    private long maxExecTime;
    private ScmTaskConfig taskConfig;
    private BSONObject extraInfo;
    private ScmWorkspace workspace;
    private ScmSession session;
    private String detail;

    public ScmTask(BSONObject info, ScmWorkspace workspace, ScmSession session)
            throws ScmException {
        basic = new ScmTaskBasicInfo(info);
        this.workspace = workspace;
        this.session = session;

        Object temp = null;

        temp = info.get(FieldName.Task.FIELD_SERVER_ID);
        if (null != temp) {
            serverId = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_PROGRESS);
        if (null != temp) {
            progress = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_SCOPE);
        if (null != temp) {
            scope = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_CONTENT);
        if (null != temp) {
            content = (BSONObject) temp;
        }

        temp = info.get(FieldName.Task.FIELD_STOP_TIME);
        if (null != temp) {
            stopTime = new Date((Long) temp);
        }

        temp = info.get(FieldName.Task.FIELD_ESTIMATE_COUNT);
        if (temp instanceof Long) {
            estimateCount = (Long) temp;
        }
        else if (null != temp) {
            estimateCount = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_ACTUAL_COUNT);
        if (temp instanceof Long) {
            actualCount = (Long) temp;
        }
        else if (null != temp) {
            actualCount = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_SUCCESS_COUNT);
        if (temp instanceof Long) {
            successCount = (Long) temp;
        }
        else if (null != temp) {
            successCount = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_FAIL_COUNT);
        if (temp instanceof Long) {
            failCount = (Long) temp;
        }
        else if (null != temp) {
            failCount = (Integer) temp;
        }

        maxExecTime = BsonUtils.getNumberOrElse(info, FieldName.Task.FIELD_MAX_EXEC_TIME, 0)
                .longValue();

        temp = info.get(FieldName.Task.FIELD_EXTRA_INFO);
        if (temp != null) {
            extraInfo = (BSONObject) temp;
        }

        temp = info.get(FieldName.Task.FIELD_DETAIL);
        if (temp != null) {
            detail = (String) temp;
        }

        if (session != null) {
            temp = info.get(FieldName.Task.FIELD_OPTION);
            if (getType() == CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE) {
                taskConfig = createTransferTaskConfig((BSONObject) temp);
            }
            else if (getType() == CommonDefine.TaskType.SCM_TASK_CLEAN_FILE) {
                taskConfig = createCleanTaskConfig((BSONObject) temp);
            }
            else if (getType() == CommonDefine.TaskType.SCM_TASK_MOVE_FILE) {
                taskConfig = createMoveFileTaskConfig((BSONObject) temp);
            }
            else if (getType() == CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE) {
                taskConfig = createSpaceRecyclingTaskConfig((BSONObject) temp);
            }
        }

    }

    private ScmTaskConfig createSpaceRecyclingTaskConfig(BSONObject option) throws ScmException {
        ScmSpaceRecyclingTaskConfig spaceRecyclingTaskConfig = new ScmSpaceRecyclingTaskConfig();
        spaceRecyclingTaskConfig.setMaxExecTime(getMaxExecTime());
        spaceRecyclingTaskConfig
                .setWorkspace(workspace != null ? workspace
                        : ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session));
        spaceRecyclingTaskConfig.setRecycleScope(ScmSpaceRecycleScope.getScope(
                BsonUtils.getStringChecked(option, FieldName.Task.FIELD_OPTION_RECYCLE_SCOPE)));
        return spaceRecyclingTaskConfig;
    }

    private ScmTaskConfig createMoveFileTaskConfig(BSONObject option) throws ScmException {
        ScmMoveTaskConfig moveTaskConfig = new ScmMoveTaskConfig();
        moveTaskConfig.setWorkspace(ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session));
        moveTaskConfig.setCondition(getContent());
        moveTaskConfig.setMaxExecTime(getMaxExecTime());
        moveTaskConfig.setTargetSite(getSiteName(basic.getTargetSite()));
        moveTaskConfig.setScope(ScmType.ScopeType.getScopeType(getScope()));
        if (option != null) {
            moveTaskConfig.setRecycleSpace(BsonUtils.getBooleanOrElse(option,
                    FieldName.Task.FIELD_OPTION_IS_RECYCLE_SPACE, false));
            moveTaskConfig.setQuickStart(BsonUtils.getBooleanOrElse(option,
                    FieldName.Task.FIELD_OPTION_QUICK_START, false));
            moveTaskConfig.setDataCheckLevel(ScmDataCheckLevel.getType(
                    BsonUtils.getStringOrElse(option, FieldName.Task.FIELD_OPTION_DATA_CHECK_LEVEL,
                            ScmDataCheckLevel.WEEK.getName())));
        }
        return moveTaskConfig;
    }

    private ScmTaskConfig createTransferTaskConfig(BSONObject option) throws ScmException {
        ScmTransferTaskConfig transferTaskConfig = new ScmTransferTaskConfig();
        transferTaskConfig.setCondition(getContent());
        transferTaskConfig.setScope(ScmType.ScopeType.getScopeType(getScope()));
        transferTaskConfig.setTargetSite(getSiteName(basic.getTargetSite()));
        transferTaskConfig
                .setWorkspace(workspace != null ? workspace
                        : ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session));
        transferTaskConfig.setMaxExecTime(getMaxExecTime());
        if (option != null) {
            transferTaskConfig.setQuickStart(BsonUtils.getBooleanOrElse(option,
                    FieldName.Task.FIELD_OPTION_QUICK_START, false));
            transferTaskConfig.setDataCheckLevel(ScmDataCheckLevel.getType(
                    BsonUtils.getStringOrElse(option, FieldName.Task.FIELD_OPTION_DATA_CHECK_LEVEL,
                            ScmDataCheckLevel.WEEK.getName())));
        }
        return transferTaskConfig;
    }

    private ScmTaskConfig createCleanTaskConfig(BSONObject option) throws ScmException {
        ScmCleanTaskConfig cleanTaskConfig = new ScmCleanTaskConfig();
        cleanTaskConfig
                .setWorkspace(workspace != null ? workspace
                        : ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session));
        cleanTaskConfig.setMaxExecTime(getMaxExecTime());
        cleanTaskConfig.setCondition(getContent());
        cleanTaskConfig.setScope(ScmType.ScopeType.getScopeType(getScope()));
        if (option != null) {
            cleanTaskConfig.setRecycleSpace(BsonUtils.getBooleanOrElse(option,
                    FieldName.Task.FIELD_OPTION_IS_RECYCLE_SPACE, false));
            cleanTaskConfig.setQuickStart(BsonUtils.getBooleanOrElse(option,
                    FieldName.Task.FIELD_OPTION_QUICK_START, false));
            cleanTaskConfig.setDataCheckLevel(ScmDataCheckLevel.getType(
                    BsonUtils.getStringOrElse(option, FieldName.Task.FIELD_OPTION_DATA_CHECK_LEVEL,
                            ScmDataCheckLevel.WEEK.getName())));
        }
        return cleanTaskConfig;
    }

    private String getSiteName(int targetSite) throws ScmException {
        ScmCursor<ScmSiteInfo> cursor = null;
        try {
            cursor = ScmFactory.Site.listSite(session);
            while (cursor.hasNext()) {
                ScmSiteInfo siteInfo = cursor.getNext();
                if (siteInfo.getId() == targetSite) {
                    return siteInfo.getName();
                }
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    // for compatible
    @Deprecated
    public ScmTask(BSONObject info) throws ScmException {
        this(info, null, null);
    }

    /**
     * Returns the value of the content property.
     *
     * @return Task content.
     * @since 2.1
     */
    public BSONObject getContent() {
        return content;
    }

    /**
     * Returns the value of the serverId property.
     *
     * @return Server id.
     * @since 2.1
     */
    public int getServerId() {
        return serverId;
    }

    /**
     * Returns the value of the scope property.
     *
     * @return scope.
     * @since 3.1
     */
    public int getScope() {
        return scope;
    }

    /**
     * Returns the value of the percentage progress property.
     *
     * @return progress
     * @since 2.1
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Returns the value of the startTime property.
     *
     * @return Start time.
     * @since 2.1
     */
    public Date getStartTime() {
        return basic.getStartTime();
    }

    /**
     * Returns the value of the stopTime property.
     *
     * @return Stop time.
     * @since 2.1
     */
    public Date getStopTime() {
        return stopTime;
    }

    /**
     * Returns the type of task.
     *
     * @return Task type.
     * @see com.sequoiacm.common.CommonDefine.TaskType
     */
    public int getType() {
        return basic.getType();
    }

    /**
     * Return the id of task.
     *
     * @return Task id.
     */
    public ScmId getId() {
        return basic.getId();
    }

    /**
     * Return the workspace name.
     *
     * @return Workspace name.
     */
    public String getWorkspaceName() {
        return basic.getWorkspaceName();
    }

    /**
     * Return the running flag of task.
     *
     * @return running flag
     * @see com.sequoiacm.common.CommonDefine.TaskRunningFlag
     */
    public int getRunningFlag() {
        return basic.getRunningFlag();
    }

    /**
     * Return the estimate count of files to be processed.
     *
     * @return Estimate count of files.
     */
    public long getEstimateCount() {
        return estimateCount;
    }

    /**
     * Return the actual count of files to be processed.
     *
     * @return Actual count of files.
     *
     */
    public long getActualCount() {
        return actualCount;
    }

    /**
     * Return the count of files successfully processed.
     *
     * @return The count of files successfully processed.
     */
    public long getSuccessCount() {
        return successCount;
    }

    /**
     * Return the count of files unsuccessfully processed.
     *
     * @return The count of files unsuccessfully processed.
     */
    public long getFailCount() {
        return failCount;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Return the config of task.
     *
     * @return Task extra info.
     */
    public ScmTaskConfig getTaskConfig() {
        return taskConfig;
    }

    /**
     * Return the extra info of task.
     *
     * @return Task extra info.
     */
    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    /**
     * Return the detail of task.
     *
     * @return Task run detail info.
     */
    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.Task.FIELD_ID).append(":").append(getId().get()).append(",");
        sb.append(FieldName.Task.FIELD_TYPE).append(":").append(getType()).append(",");
        sb.append(FieldName.Task.FIELD_WORKSPACE).append(":").append(getWorkspaceName())
                .append(",");
        sb.append(FieldName.Task.FIELD_RUNNING_FLAG).append(":").append(getRunningFlag())
                .append(",");
        sb.append(FieldName.Task.FIELD_SERVER_ID).append(":").append(getServerId()).append(",");
        sb.append(FieldName.Task.FIELD_PROGRESS).append(":").append(getProgress()).append(",");
        sb.append(FieldName.Task.FIELD_CONTENT).append(":").append(getContent()).append(",");
        sb.append(FieldName.Task.FIELD_START_TIME).append(":").append(getStartTime()).append(",");
        sb.append(FieldName.Task.FIELD_STOP_TIME).append(":").append(getStopTime()).append(",");

        sb.append(FieldName.Task.FIELD_ESTIMATE_COUNT).append(":").append(estimateCount)
                .append(",");
        sb.append(FieldName.Task.FIELD_ACTUAL_COUNT).append(":").append(actualCount).append(",");
        sb.append(FieldName.Task.FIELD_SUCCESS_COUNT).append(":").append(successCount).append(",");
        sb.append(FieldName.Task.FIELD_FAIL_COUNT).append(":").append(failCount).append(",");
        sb.append(FieldName.Task.FIELD_MAX_EXEC_TIME).append(":").append(maxExecTime).append(",");
        sb.append(FieldName.Task.FIELD_EXTRA_INFO).append(":").append(extraInfo);

        sb.append(FieldName.Task.FIELD_DETAIL).append(":").append(detail);

        return sb.toString();
    }
}
