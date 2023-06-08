package com.sequoiacm.cloud.authentication.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.util.Assert;

import java.util.TimerTask;

public class SessionCleanupTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupTask.class);

    private final SequoiadbSessionRepository sessionRepository;
    private final int maxCleanupNum;

    public SessionCleanupTask(SequoiadbSessionRepository sessionRepository, int maxCleanupNum) {
        Assert.notNull(sessionRepository, "Cannot pass null sessionRepository");
        this.sessionRepository = sessionRepository;
        this.maxCleanupNum = maxCleanupNum;
    }

    @Override
    public void run() {
        try {
            int num = sessionRepository.cleanExpiredSessions(maxCleanupNum);
            logger.info("Cleanup {} expired sessions", num);
        }
        catch (Throwable e) {
            logger.error("Cleanup expired sessions failed", e);
        }
    }
}
