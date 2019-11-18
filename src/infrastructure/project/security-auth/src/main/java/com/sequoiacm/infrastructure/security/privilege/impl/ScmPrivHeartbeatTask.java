package com.sequoiacm.infrastructure.security.privilege.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

public class ScmPrivHeartbeatTask extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ScmPrivHeartbeatTask.class);

    private ScmPrivClient client;

    ScmPrivHeartbeatTask(ScmPrivClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            int version = client.getVersionFromAuthServer();
            synchronized (client) {
                if (client.getVersion() != version) {
                    logger.info("reload ScmPrivClient version={},newVersion={}",
                            client.getVersion(), version);
                    client.reload();
                }
            }
        }
        catch (Exception e) {
            logger.warn("check version from auth-server failed", e);
        }
    }

}
