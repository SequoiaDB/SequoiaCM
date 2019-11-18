package com.sequoiacm.client.element;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;

/**
 * The brief and partial information of task.
 */
public class ScmTaskBasicInfo {
    private int type;
    private ScmId id;
    private ScmId scheduleId;
    private String workspaceName;
    private int targetSite;
    private Date startTime;
    private int runningFlag;

    public ScmTaskBasicInfo(BSONObject info) throws ScmException {
        Object temp = null;

        temp = info.get(FieldName.Task.FIELD_TYPE);
        if (null != temp) {
            type = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_START_TIME);
        if (null != temp) {
            startTime = new Date((Long) temp);
        }

        temp = info.get(FieldName.Task.FIELD_TARGET_SITE);
        if (null != temp) {
            targetSite = (Integer) temp;
        }

        temp = info.get(FieldName.Task.FIELD_ID);
        if (null != temp) {
            id = new ScmId((String) temp, false);
        }

        temp = info.get(FieldName.Task.FIELD_SCHEDULE_ID);
        if (null != temp) {
            scheduleId = new ScmId((String) temp, false);
        }

        temp = info.get(FieldName.Task.FIELD_WORKSPACE);
        if (null != temp) {
            workspaceName = (String) temp;
        }

        temp = info.get(FieldName.Task.FIELD_RUNNING_FLAG);
        if (null != temp) {
            runningFlag = (Integer) temp;
        }
    }

    public ScmTaskBasicInfo() {
    }

    /**
     * Returns the value of target site id property.
     *
     * @return targetSite
     */
    public int getTargetSite() {
        return targetSite;
    }

    /**
     * Sets the value of the targetSite property.
     *
     * @param targetSite
     *            target site id.
     */
    public void setTargetSite(int targetSite) {
        this.targetSite = targetSite;
    }

    /**
     * Returns the value of start time property.
     *
     * @return startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     *
     * @param startTime
     *            start time.
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the value of type property.
     *
     * @return Task type.
     * @see com.sequoiacm.common.CommonDefine.TaskType
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param type
     *            task type.
     * @see com.sequoiacm.common.CommonDefine.TaskType
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Returns the value of id property
     *
     * @return Task id.
     */
    public ScmId getId() {
        return id;
    }

    /**
     * Sets the value of id property.
     *
     * @param id
     *            task id.
     */
    public void setId(ScmId id) {
        this.id = id;
    }

    /**
     * Returns the value of scheduleId property
     *
     * @return Task's schedule id.
     */
    public ScmId getScheduleId() {
        return scheduleId;
    }

    /**
     * Sets the value of scheduleId property.
     *
     * @param scheduleId
     *            task's schedule id.
     */
    public void setScheduleId(ScmId scheduleId) {
        this.scheduleId = scheduleId;
    }

    /**
     * Returns the value of workspaceName property.
     *
     * @return Workspace name.
     *
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Sets the value of workspaceName property.
     *
     * @param workspaceName
     *            workspace name
     */
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    /**
     * Returns the value of runningFlag property.
     *
     * @return Running flag
     * @see com.sequoiacm.common.CommonDefine.TaskRunningFlag
     */
    public int getRunningFlag() {
        return runningFlag;
    }

    /**
     * Sets the values of runningFlag property.
     *
     * @param runningFlag
     *            task running flag
     */
    public void setRunningFlag(int runningFlag) {
        this.runningFlag = runningFlag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(id.get()).append(",");
        sb.append("schduleId:").append(scheduleId).append(",");
        sb.append("type:").append(type).append(",");
        sb.append("runningFlag:").append(runningFlag).append(",");
        sb.append("workspaceName:").append(workspaceName).append(",");
        sb.append("targetSite:").append(targetSite).append(",");
        sb.append("startTime:").append(startTime);

        return sb.toString();
    }
}
