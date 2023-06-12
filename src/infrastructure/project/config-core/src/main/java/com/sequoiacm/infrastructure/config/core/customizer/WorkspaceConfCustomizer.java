package com.sequoiacm.infrastructure.config.core.customizer;

import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.springframework.stereotype.Component;

@Component
@BusinessType(ScmBusinessTypeDefine.WORKSPACE)
public class WorkspaceConfCustomizer implements ConfigCustomizer {
    @Override
    public VersionHeartbeatOption heartbeatOption() {
        VersionHeartbeatOption ret = new VersionHeartbeatOption();
        ret.setInitStatusHeartbeatInterval(2 * 1000);
        return ret;
    }

    @Override
    public String toString() {
        return "WorkspaceConfCustomizer{versionHeartBeat= " + heartbeatOption() + " }";
    }
}
