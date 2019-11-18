package com.sequoiacm.cloud.adminserver.core.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.StatisticalFileDelta;

public final class StatisticsFileDeltaJob extends StatisticsJob {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsFileDeltaJob.class);

    private boolean needBacktrace = true;
    
    private StatisticalFileDelta statisFileDelta = new StatisticalFileDelta();
    
    public StatisticsFileDeltaJob() {
    }

    @Override
    public int getType() {
        return StatisticsDefine.StatisticsType.FILE_DELTA;
    }

    @Override
    public String getName() {
        return "STATISTICS_JOB_FILE_DELTA";
    }


    @Override
    public void run() {
        try {
            this.statisFileDelta.doStatistics(needBacktrace);
            needBacktrace = false;
        }
        catch (Exception e) {
            logger.error("do file delta job failed", e);
            needBacktrace = true;
        }
    }

}
