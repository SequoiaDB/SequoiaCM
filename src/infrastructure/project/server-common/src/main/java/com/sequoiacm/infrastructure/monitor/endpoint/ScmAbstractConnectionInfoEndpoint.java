package com.sequoiacm.infrastructure.monitor.endpoint;

import com.sequoiacm.infrastructure.monitor.model.ScmConnectionInfo;
import org.springframework.boot.actuate.endpoint.Endpoint;

public abstract class ScmAbstractConnectionInfoEndpoint implements Endpoint<ScmConnectionInfo> {
    @Override
    public String getId() {
        return "connection_info";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isSensitive() {
        return false;
    }

    @Override
    public abstract ScmConnectionInfo invoke();
}
