package com.sequoiacm.schedule.core.job;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.schedule.common.FieldName;

public class InternalScheduleInfo extends ScheduleJobInfo {
    private String jobType;
    private String workerService;
    private String workerNode;
    private String workerPreferRegion;
    private String workerPreferZone;
    private BSONObject jobData;
    private long workerNodeStartTime;
    private String name;

    private boolean stop;

    public InternalScheduleInfo(String id, String name, String type, String workspace,
            BSONObject content, String cron) {
        super(id, type, workspace, cron);
        this.name = name;
        jobType = BsonUtils.getStringChecked(content, FieldName.Schedule.FIELD_INTERNAL_JOB_TYPE);
        workerService = BsonUtils.getStringChecked(content,
                FieldName.Schedule.FIELD_INTERNAL_WORKER_SERVICE);
        workerNode = BsonUtils.getString(content, FieldName.Schedule.FIELD_INTERNAL_WORKER_NODE);
        workerNodeStartTime = BsonUtils
                .getNumberOrElse(content, FieldName.Schedule.FIELD_INTERNAL_WORKER_START_TIME, 0)
                .longValue();
        jobData = BsonUtils.getBSONChecked(content, FieldName.Schedule.FIELD_INTERNAL_JOB_DATA);
        workerPreferRegion = BsonUtils.getString(content,
                FieldName.Schedule.FIELD_INTERNAL_WORKER_PREFER_REGION);
        workerPreferZone = BsonUtils.getString(content,
                FieldName.Schedule.FIELD_INTERNAL_WORKER_PREFER_ZONE);

    }

    public String getName() {
        return name;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public String getWorkerPreferRegion() {
        return workerPreferRegion;
    }

    public String getWorkerPreferZone() {
        return workerPreferZone;
    }

    public void setWorkerPreferRegion(String workerPreferRegion) {
        this.workerPreferRegion = workerPreferRegion;
    }

    public void setWorkerPreferZone(String workerPreferZone) {
        this.workerPreferZone = workerPreferZone;
    }

    public long getWorkerNodeStartTime() {
        return workerNodeStartTime;
    }

    public void setWorkerNodeStartTime(long workerNodeStartTime) {
        this.workerNodeStartTime = workerNodeStartTime;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getWorkerService() {
        return workerService;
    }

    public void setWorkerService(String workerService) {
        this.workerService = workerService;
    }

    public String getWorkerNode() {
        return workerNode;
    }

    public void setWorkerNode(String workerNode) {
        this.workerNode = workerNode;
    }

    public BSONObject getJobData() {
        return jobData;
    }

    public void setJobData(BSONObject jobData) {
        this.jobData = jobData;
    }

    @Override
    public String toString() {
        return "InternalScheduleInfo [jobType=" + jobType + ", workerService=" + workerService
                + ", workerNode=" + workerNode + ", workerPreferRegion=" + workerPreferRegion
                + ", workerPreferZone=" + workerPreferZone + ", jobData=" + jobData + ", getId()="
                + getId() + ", getType()=" + getType() + ", getWorkspace()=" + getWorkspace()
                + ", getCron()=" + getCron() + "]";
    }
}
