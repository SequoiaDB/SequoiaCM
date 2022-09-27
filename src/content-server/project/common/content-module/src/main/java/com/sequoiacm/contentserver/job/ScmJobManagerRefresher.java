package com.sequoiacm.contentserver.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmJobManagerRefresher {

    private static final Logger logger = LoggerFactory.getLogger(ScmJobManagerRefresher.class);

    public static void refreshThreadPoolConfig(int coreSize, int maxSize) {
        ScmJobManager jobManager = ScmJobManager.getInstance();
        if (jobManager.getCoreThreadSize() != coreSize
                || jobManager.getMaxThreadSize() != maxSize) {
            jobManager.updateThreadPoolConfig(coreSize, maxSize);
            logger.info("update scmJobManager config:coreSize={},maxSize={}", coreSize, maxSize);
        }
    }
}
