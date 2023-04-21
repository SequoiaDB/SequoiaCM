package com.sequoiacm.cloud.adminserver.core.job;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.StatisticsObjectDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsObjectDeltaJob extends StatisticsJob {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsObjectDeltaJob.class);

    // 避免对数据库造成太大压力，定时任务在统计时，不需要修复之前的数据；用户可通过 refresh 接口手动修复
    private boolean needBacktrace = false;

    private StatisticsObjectDelta statisticsObjectDelta = new StatisticsObjectDelta();

    @Override
    public int getType() {
        return StatisticsDefine.StatisticsType.OBJECT_DELTA;
    }

    @Override
    public String getName() {
        return "STATISTICS_JOB_OBJECT_DELTA";
    }

    @Override
    public void run() {
        try {
            this.statisticsObjectDelta.doStatistics(needBacktrace);
        }
        catch (Exception e) {
            logger.error("do object delta job failed", e);
        }
    }
}
