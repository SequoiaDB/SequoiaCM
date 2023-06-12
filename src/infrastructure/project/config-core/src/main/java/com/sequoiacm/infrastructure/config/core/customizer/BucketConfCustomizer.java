package com.sequoiacm.infrastructure.config.core.customizer;

import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigDefine;
import org.springframework.stereotype.Component;

@Component
@BusinessType(ScmBusinessTypeDefine.BUCKET)
public class BucketConfCustomizer implements ConfigCustomizer {
    @Override
    public VersionHeartbeatOption heartbeatOption() {
        VersionHeartbeatOption ret = new VersionHeartbeatOption();
        ret.setGlobalVersionHeartbeat(true);
        ret.setGlobalVersionName(BucketConfigDefine.ALL_BUCKET_VERSION);
        return ret;
    }

    @Override
    public String toString() {
        return "BucketConfCustomizer{versionHeartBeat= " + heartbeatOption() + " }";
    }
}
