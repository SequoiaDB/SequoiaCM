package com.sequoiacm.schedule.core.job;

import java.util.List;

public interface ScheduleMgr {
    public void createJob(ScheduleJobInfo info) throws Exception;

    public void deleteJob(String id) throws Exception;

    public void start() throws Exception;

    public void clear();

    public ScheduleJobInfo getJobInfo(String id);

    public List<ScheduleJobInfo> ListJob();
}
