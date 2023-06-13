package com.sequoiacm.config.server.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScmConfProp {

    @JsonProperty("service_name")
    private String serviceName;

    @JsonProperty("instance")
    private String instance;

    @JsonProperty("config_key")
    private String configKey;

    @JsonProperty("config_value")
    private String configValue;

    public ScmConfProp() {
    }

    public ScmConfProp(String serviceName, String instance, String configKey, String configValue) {
        this.serviceName = serviceName;
        this.instance = instance;
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    @Override
    public String toString() {
        return "ScmConfProps{" + "serviceName='" + serviceName + '\'' + ", instanceUrl='" + instance
                + '\'' + ", configKey='" + configKey + '\'' + ", configValue='" + configValue + '\''
                + '}';
    }
}
