package com.sequoiacm.schedule.client.worker;

import java.util.concurrent.CountDownLatch;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.client.ScheduleClient;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleException;

public abstract class ScheduleWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleWorker.class);
    private ScheduleClient schClinet;
    private BSONObject jobDataBson;
    private InternalSchStatus status;
    private boolean hasReportFinish = false;
    private ScheduleWorkerMgr workerMgr;
    private CountDownLatch exitLatch = new CountDownLatch(1);
    private String schName;
    private volatile boolean isStop;

    @Override
    public void run() {
        logger.info("schedule worker start:schId={}, schName={}", getScheduleId(),
                getScheduleName());
        try {
            exec(schName, jobDataBson);
            if (!hasReportFinish && !isStop()) {
                logger.debug("auto report finish:schId={}, startTime={}, jobData={}",
                        status.getSchId(), status.getStartTime(), jobDataBson);
                reportStatus(null, true);
            }
        }
        catch (Exception e) {
            logger.error("failed to exec schedule job:schId={}, startTime={}, jobData={}",
                    status.getSchId(), status.getStartTime(), jobDataBson, e);
        }
        finally {
            try {
                close();
            }
            catch (Exception e) {
                logger.warn("failed to close schedule job:schId={}, startTime={}, jobData={}",
                        status.getSchId(), status.getStartTime(), jobDataBson, e);
            }
            exitLatch.countDown();
            workerMgr.workerExit(status.getSchId());
            logger.info("schedule worker exit:schId={}, schName={}", getScheduleId(),
                    getScheduleName());
        }
    }

    protected abstract void exec(String schName, BSONObject jobData) throws Exception;

    public boolean isStop() {
        return isStop;
    }

    protected void reportStatus(BSONObject statusInfo, boolean isFinish) throws ScheduleException {
        if (isFinish) {
            hasReportFinish = true;
        }
        status.setFinish(isFinish);
        status.setStatus(statusInfo);
        schClinet.reportStatus(status);
    }

    void injectWorkerArg(ScheduleWorkerMgr workerMgr, ScheduleClient schClinet, String schId,
            String schName, String workerNode, long startTime, BSONObject jobData) {
        this.schClinet = schClinet;
        this.jobDataBson = jobData;
        this.workerMgr = workerMgr;
        this.schName = schName;
        this.status = new InternalSchStatus();

        status.setFinish(false);
        status.setSchId(schId);
        status.setSchName(schName);
        status.setStartTime(startTime);
        status.setWorkerNode(workerNode);
    }

    void waitExit() throws InterruptedException {
        exitLatch.await();
    }

    protected void close() throws Exception {

    }

    protected String getScheduleName() {
        return schName;
    }

    protected String getScheduleId() {
        return status.getSchId();
    }

    void stop() {
        isStop = true;
    }
}
