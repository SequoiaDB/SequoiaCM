package com.sequoiacm.schedule.client.worker;

public interface ScheduleWorkerBuilder {
    public String getJobType();

    public ScheduleWorker createWorker();
}
