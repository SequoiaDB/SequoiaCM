package com.sequoiacm.schedule.core.elect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.vote.ScmNotLeaderAction;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;

public class ScheduleNotLeaderAction implements ScmNotLeaderAction {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleNotLeaderAction.class);

    private ScheduleElector elector;

    public ScheduleNotLeaderAction(ScheduleElector elector) {
        this.elector = elector;
    }

    @Override
    public void run() {
        logger.info("################not leader#######################");
        ScheduleMgrWrapper.getInstance().clear();
    }

}