package com.sequoiacm.deploy.core;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.deploy.common.RestTools;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.infrastructure.common.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ScmGlobalConfigSetter {
    private static final Logger logger = LoggerFactory.getLogger(ScmGlobalConfigSetter.class);
    private final Map<String, String> globalConfig;
    private final String scmPassword;
    private final String scmUser;
    private final String gatewayUrl;

    public ScmGlobalConfigSetter(Map<String, String> globalConfig, String gatewayUrl,
            String scmUser,
            String scmPassword) {
        this.globalConfig = globalConfig;
        this.scmPassword = scmPassword;
        this.scmUser = scmUser;
        this.gatewayUrl = gatewayUrl;
    }

    public void setGlobalConfigSilence() throws Exception {
        ScmSession session = null;
        try {
            RestTools.waitDependentServiceReady(gatewayUrl,
                    CommonConfig.getInstance().getWaitServiceReadyTimeout(), "auth-server",
                    "config-server");
            session = ScmFactory.Session
                    .createSession(new ScmConfigOption(gatewayUrl, scmUser, scmPassword));

            for (Map.Entry<String, String> entry : globalConfig.entrySet()) {
                ScmSystem.Configuration.setGlobalConfig(session, entry.getKey(), entry.getValue());
            }
            logger.info("Set global config success");
        }
        catch (Exception e) {
            logger.warn(
                    "failed to set global config, please use config server admin tools to retry later: {}",
                    globalConfig, e);
        }
        finally {
            IOUtils.close(session);
        }
    }
}
