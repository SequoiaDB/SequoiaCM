package com.sequoiacm.config.framework.event;

import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class ScmConfEventBase implements ScmConfEvent {
    private NotifyOption notifyOption;
    private String configName;

    public ScmConfEventBase(String configName, NotifyOption notifycation) {
        this.configName = configName;
        this.notifyOption = notifycation;
    }

    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public NotifyOption getNotifyOption() {
        return notifyOption;
    }

    @Override
    public String toString() {
        return "configName=" + configName + ",notifyOption=" + notifyOption;
    }

}
