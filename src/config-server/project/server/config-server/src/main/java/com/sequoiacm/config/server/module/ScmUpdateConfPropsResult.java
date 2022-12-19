package com.sequoiacm.config.server.module;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ScmUpdateConfPropsResult {
    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_SERVICE)
    private String serviceName;

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_INSTANCE)
    private String instanceUrl;

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_MESSAGE)
    private String errorMessage;

    private boolean isSuccess;

    private Set<String> rebootConf;

    private Map<String, String> adjustConf;

    public ScmUpdateConfPropsResult(String serviceName, String instance, Set<String> rebootConf,
            Map<String, String> adjustConf) {
        super();
        this.serviceName = serviceName;
        this.instanceUrl = instance;
        this.isSuccess = true;
        this.rebootConf = rebootConf == null ? Collections.<String> emptySet() : rebootConf;
        this.adjustConf = adjustConf == null ? Collections.<String, String> emptyMap() : adjustConf;
    }

    public ScmUpdateConfPropsResult(String serviceName, String instance, String errorMessage) {
        super();
        this.serviceName = serviceName;
        this.instanceUrl = instance;
        if (errorMessage == null) {
            errorMessage = "unknown error";
        }
        this.errorMessage = errorMessage;

        this.isSuccess = false;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instance) {
        this.instanceUrl = instance;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message) {
        this.errorMessage = message;
    }

    @Override
    public String toString() {
        return "ScmUpdateConfPropsResult{" + "serviceName='" + serviceName + '\''
                + ", instanceUrl='" + instanceUrl + '\'' + ", errorMessage='" + errorMessage + '\''
                + ", isSuccess=" + isSuccess + ", rebootConf=" + rebootConf + ", adjustConf="
                + adjustConf + '}';
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Set<String> getRebootConf() {
        return rebootConf;
    }

    public Map<String, String> getAdjustConf() {
        return adjustConf;
    }
}
