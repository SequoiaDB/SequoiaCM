package com.sequoiacm.cloud.adminserver.core.job;

import com.sequoiacm.cloud.adminserver.dao.BreakpointFileStatisticsDao;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreakpointFileCleanJob extends ScmTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(BreakpointFileCleanJob.class);

    private BreakpointFileStatisticsDao dao;
    private long maxStayDay;


    public BreakpointFileCleanJob(BreakpointFileStatisticsDao dao, long maxStayDay) {
        this.dao = dao;
        this.maxStayDay = maxStayDay;
    }

    @Override
    public void run() {
        try {
            logger.info("start cleaning breakpointFile statistics data, maxStayDay:" + maxStayDay);
            dao.clearRecords(maxStayDay);
            logger.info("successfully cleared breakpointFile statistics data");
        }
        catch (Exception e) {
            logger.info("failed to clear breakpointFile statistics data", e);
        }

    }
}
