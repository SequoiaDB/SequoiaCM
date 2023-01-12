package com.sequoiacm.schedule.core.elect;

import com.sequoiacm.schedule.core.ScmCheckCorrectionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import com.sequoiacm.infrastructure.vote.ScmLeaderAction;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;

public class ScheduleLeaderAction implements ScmLeaderAction {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleLeaderAction.class);

    private ScheduleElector elector;
    private long lastFailedTime;
    private BackOffExecution backOffExecution;

    private BackOff backOff;

    private static final long threshold = 15000;

    public ScheduleLeaderAction(ScheduleElector elector, long revoteInitialInterval,
            long revoteMaxInterval, double revoteIntervalMultiplier) {
        logger.info("revoteInitialInterval={},revoteMaxInterval={},revoteIntervalMultiplier={}",
                revoteInitialInterval, revoteMaxInterval, revoteIntervalMultiplier);
        this.elector = elector;
        ExponentialBackOff backOff = new ExponentialBackOff(revoteInitialInterval,
                revoteIntervalMultiplier);
        backOff.setMaxInterval(revoteMaxInterval);
        this.backOff = backOff;
        this.backOffExecution = backOff.start();
    }

    @Override
    public synchronized void run() {
        long leaderActionTime = System.currentTimeMillis();
        try {
            logger.info("################leader init#######################");
            ScheduleMgrWrapper.getInstance().start();
            // 生命周期管理检查和修复
            ScmCheckCorrectionTools.getInstance().checkAndCorrection();
            lastFailedTime = 0;
            backOffExecution = backOff.start();

        }
        catch (Exception e) {
            if(lastFailedTime > 0 && leaderActionTime - lastFailedTime > threshold) {
                // last failure was a long time ago, just start a new backoff.
                backOffExecution = backOff.start();
            }
            long delayToReconnect = backOffExecution.nextBackOff();

            logger.warn("failed to init ScheduleMgr, launch an revote and I will become slient "
                    + delayToReconnect + "ms", e);

            try {
                elector.quitAndReVote(delayToReconnect);
            }
            catch (Exception e1) {
                logger.error("revote failed", e);
                ScheduleCommonTools.exitProcess();
            }

            lastFailedTime = System.currentTimeMillis();
        }
    }
}
