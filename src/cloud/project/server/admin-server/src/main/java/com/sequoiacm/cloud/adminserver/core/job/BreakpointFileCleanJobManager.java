package com.sequoiacm.cloud.adminserver.core.job;

import com.sequoiacm.cloud.adminserver.dao.BreakpointFileStatisticsDao;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreakpointFileCleanJobManager {

    private static final Logger logger = LoggerFactory
            .getLogger(BreakpointFileCleanJobManager.class);

    private final static BreakpointFileCleanJobManager INSTANCE = new BreakpointFileCleanJobManager();
    private ScmTimer jobTimer = ScmTimerFactory.createScmTimer();

    private BreakpointFileCleanJobManager() {
    }

    public static BreakpointFileCleanJobManager getInstance() {
        return INSTANCE;
    }

    public void startCleanJob(BreakpointFileStatisticsDao dao, long period, long maxStayDay) {
        jobTimer.schedule(new BreakpointFileCleanJob(dao, maxStayDay), 0, period);
        logger.info("start breakpointFile statistics data cleanup job success: " + "period=" + period
                + ",maxStayDay=" + maxStayDay);
    }

}
