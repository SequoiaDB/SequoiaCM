package com.sequoiacm.fulltext.server.sch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.client.worker.ScheduleWorker;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;

public abstract class WorkspaceIdxWorkerBase extends ScheduleWorker {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceIdxWorkerBase.class);

    private SchJobStatus statusInfo;
    private long lastReportStatusTime;
    private IdxTaskContext context;

    private IdxThreadPool taskMgr;

    public WorkspaceIdxWorkerBase(IdxThreadPool taskMgr) {
        this.taskMgr = taskMgr;
        statusInfo = new SchJobStatus(0, 0, 0, 0);
        context = new IdxTaskContext();
    }

    protected SchJobStatus getStatus() {
        return statusInfo;
    }

    public IdxTaskContext getTaskContext() {
        return context;
    }

    protected void reportInitStatus() throws ScheduleException {
        logger.debug("report init status:{}", statusInfo.toBsonObject());
        lastReportStatusTime = System.currentTimeMillis();
        reportStatus(statusInfo.toBsonObject(), false);
    }

    public void reportStatus(boolean isFinish) throws ScheduleException {
        long processCount = context.getErrorCount() + context.getSuccessCount();
        long delta = processCount - (statusInfo.getErrorCount() + statusInfo.getSuccessCount());
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastReportStatusTime;
        if (isFinish) {
            statusInfo.setEstimateCount(processCount);
        }
        if ((delta > 50 || elapsedTime > 5000) || isFinish) {
            statusInfo.setErrorCount(context.getErrorCount());
            statusInfo.setSuccessCount(context.getSuccessCount());
            if (delta > 0) {
                float elapsedTimeSec = ((float) elapsedTime) / 1000;
                float speed = delta / elapsedTimeSec;
                statusInfo.setSpeed(speed);
            }

            try {
                logger.debug("report status:{}, isFinish={}", statusInfo.toBsonObject(), isFinish);
                reportStatus(statusInfo.toBsonObject(), isFinish);
            }
            catch (ScheduleException e) {
                if (e.getCode().equals(RestCommonDefine.ErrorCode.WORKER_SHOULD_STOP)) {
                    stop();
                    throw e;
                }
                logger.warn("failed to report job status:{}, jobStatus={}", toString(),
                        statusInfo.toBsonObject(), e);
            }
            lastReportStatusTime = now;
        }
    }

    protected void submit(Runnable task) {
        taskMgr.submit(task);
    }

    @Override
    protected void close() throws Exception {
        super.close();
    }

}
