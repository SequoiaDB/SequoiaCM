package com.sequoiacm.cloud.authentication.task;

import com.sequoiacm.cloud.authentication.config.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.stereotype.Component;

import java.util.Timer;

@Component
public class SessionCleanupTaskRunner implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupTaskRunner.class);
    private Timer timer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        ApplicationContext context = event.getApplicationContext();

        SessionConfig sessionConfig = context.getBean(SessionConfig.class);
        if (sessionConfig == null) {
            throw new RuntimeException("No SessionConfig bean found");
        }

        SequoiadbSessionRepository sessionRepository = context.getBean(SequoiadbSessionRepository.class);
        if (sessionRepository == null) {
            throw new RuntimeException("No SequoiadbSessionRepository bean found");
        }

        if (sessionConfig.getCleanInactiveInterval() == 0) {
            logger.info("No need to clean expired sessions due to configuration");
            return;
        }

        SessionCleanupTask task = new SessionCleanupTask(
                sessionRepository,
                sessionConfig.getMaxCleanupNum());

        timer = new Timer("SessionCleanup", true);
        timer.schedule(task, 0, sessionConfig.getCleanInactiveInterval() * 1000);
        logger.info("Session cleanup task is running");
    }
}
