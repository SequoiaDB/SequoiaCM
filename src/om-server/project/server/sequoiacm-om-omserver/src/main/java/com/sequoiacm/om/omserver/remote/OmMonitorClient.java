package com.sequoiacm.om.omserver.remote;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.om.omserver.module.monitor.OmConnectionInfo;
import com.sequoiacm.om.omserver.module.monitor.OmMonitorInstanceInfo;
import com.sequoiacm.om.omserver.module.monitor.OmProcessInfo;
import com.sequoiacm.om.omserver.module.monitor.OmThreadInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import java.util.Map;

public class OmMonitorClient {

    private static final String PROPERTY_ACTUATOR_SECURITY_ENABLED = "actuatorSecurityEnabled";

    private final ScmOmSession session;

    private final OmMonitorFeignClient feignClient;

    private boolean needAuth;

    public OmMonitorClient(OmMonitorInstanceInfo instanceInfo, ScmOmSession session,
            OmMonitorFeignClient feignClient) {
        this.session = session;
        this.feignClient = feignClient;
        if (instanceInfo.getMetadata() != null) {
            this.needAuth = Boolean.parseBoolean(String
                    .valueOf(instanceInfo.getMetadata().get(PROPERTY_ACTUATOR_SECURITY_ENABLED)));
        }
    }

    public OmConnectionInfo getConnectionInfo() throws ScmFeignException {
        if (needAuth) {
            return feignClient.getConnectionInfoWithAuth(session.getSessionId());
        }
        return feignClient.getConnectionInfo();
    }

    public OmThreadInfo getThreadInfo() throws ScmFeignException {
        if (needAuth) {
            return feignClient.getThreadInfoWithAuth(session.getSessionId());
        }
        return feignClient.getThreadInfo();
    }

    public Map<String, Object> getHeapInfo() throws ScmFeignException {
        if (needAuth) {
            return feignClient.getHeapInfoWithAuth(session.getSessionId());
        }
        return feignClient.getHeapInfo();
    }

    public OmProcessInfo getProcessInfo() throws ScmFeignException {
        if (needAuth) {
            return feignClient.getProcessInfoWithAuth(session.getSessionId());
        }
        return feignClient.getProcessInfo();
    }

    public Map<?, ?> getEnvironmentInfo() throws ScmFeignException {
        if (needAuth) {
            return feignClient.getEnvironmentInfoWithAuth(session.getSessionId());
        }
        return feignClient.getEnvironmentInfo();
    }
}
