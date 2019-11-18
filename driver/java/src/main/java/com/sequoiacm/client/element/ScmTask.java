package com.sequoiacm.client.element;

import java.util.Date;

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
    private Date stopTime;
    private long estimateCount;
    private long actualCount;
    private long successCount;
    private long failCount;
    private long maxExecTime;

    public ScmTask(BSONObject info) throws ScmException {
        basic = new ScmTaskBasicInfo(info);

        Object temp = null;

        temp = info.get(FieldName.Task.FIELD_SERVER_ID);
        if (null != temp) {
            serverId = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_PROGRESS);
        if (null != temp) {
            progress = (Integer) temp;
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
        sb.append(FieldName.Task.FIELD_MAX_EXEC_TIME).append(":").append(maxExecTime);

        return sb.toString();
    }
}
