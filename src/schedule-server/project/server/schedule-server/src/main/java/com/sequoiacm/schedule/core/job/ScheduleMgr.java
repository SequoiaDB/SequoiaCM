package com.sequoiacm.schedule.core.job;

import java.util.List;

public interface ScheduleMgr {

    // 保证后续的 createJob 不会因为参数错误而失败
    public SchJobCreateContext prepareCreateJob(ScheduleJobInfo info) throws Exception;

    public void createJob(SchJobCreateContext contex) throws Exception;

    public void deleteJob(String id, boolean stopWorker) throws Exception;

    public void start() throws Exception;

    public void clear();

    public ScheduleJobInfo getJobInfo(String id);

    public List<ScheduleJobInfo> ListJob();
}
