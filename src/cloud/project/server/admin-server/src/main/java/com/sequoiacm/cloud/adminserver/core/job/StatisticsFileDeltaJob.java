package com.sequoiacm.cloud.adminserver.core.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.StatisticalFileDelta;

public final class StatisticsFileDeltaJob extends StatisticsJob {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsFileDeltaJob.class);

    // 避免对数据库造成太大压力，定时任务在统计时，不需要修复之前的数据；用户可通过 refresh 接口手动修复
    private boolean needBacktrace = false;
    
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
        }
        catch (Exception e) {
            logger.error("do file delta job failed", e);
        }
    }

}
