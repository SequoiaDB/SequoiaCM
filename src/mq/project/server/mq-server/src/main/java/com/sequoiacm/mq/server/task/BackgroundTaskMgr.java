package com.sequoiacm.mq.server.task;

import java.util.List;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;

@Component
public class BackgroundTaskMgr {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundTaskMgr.class);
    private ScmTimer timer = ScmTimerFactory.createScmTimer();

    @Autowired
    public BackgroundTaskMgr(List<BackgroundJob> jobs) {
        for (BackgroundJob job : jobs) {
            logger.info("{} job start, period={}ms", job.getJobName(), job.getPeriod());
            timer.schedule(job, 0, job.getPeriod());
        }
    }

    @PreDestroy
    public void destory() {
        timer.cancel();
    }
}
