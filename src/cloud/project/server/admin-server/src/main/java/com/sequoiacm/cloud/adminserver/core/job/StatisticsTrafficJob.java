package com.sequoiacm.cloud.adminserver.core.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.StatisticalTraffic;

public final class StatisticsTrafficJob extends StatisticsJob {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsTrafficJob.class);

    private boolean needBacktrace = true;
    
    private StatisticalTraffic statisTraffic = new StatisticalTraffic();
    
    public StatisticsTrafficJob() {
    }
    
    @Override
    public int getType() {
        return StatisticsDefine.StatisticsType.TRAFFIC;
    }

    @Override
    public String getName() {
        return "STATISTICS_JOB_TRAFFIC";
    }
    
    @Override
    public void run() {
        try {
            this.statisTraffic.doStatistics(needBacktrace);
            needBacktrace = false;
        }
        catch (Exception e) {
            logger.error("do traffic job failed", e);
            needBacktrace = true;
        }
    }

}
