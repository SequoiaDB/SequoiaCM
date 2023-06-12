package com.sequoiacm.config.framework.subscriber;

public class ScmConfSubscriber {

    private String configName;
    private String serviceName;

    public ScmConfSubscriber(String configName, String serviceName) {
        this.configName = configName;
        this.serviceName = serviceName;
    }

    public String getConfigName() {
        return configName;
    }

    public String getServiceName() {
        return serviceName;
    }

}
