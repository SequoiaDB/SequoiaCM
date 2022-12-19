package com.sequoiacm.infrastructure.config.core.common;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ScmServiceUpdateConfigResult {
    private Set<String> rebootConf = Collections.emptySet();
    private Map<String, String> adjustedConf = Collections.emptyMap();

    public Set<String> getRebootConf() {
        return rebootConf;
    }

    public void setRebootConf(Set<String> rebootConf) {
        this.rebootConf = rebootConf;
    }

    public Map<String, String> getAdjustedConf() {
        return adjustedConf;
    }

    public void setAdjustedConf(Map<String, String> adjustedConf) {
        this.adjustedConf = adjustedConf;
    }

    @Override
    public String toString() {
        return "ScmUpdateConfigResult{" + "rebootConf=" + rebootConf + ", adjustedConf="
                + adjustedConf + '}';
    }
}
