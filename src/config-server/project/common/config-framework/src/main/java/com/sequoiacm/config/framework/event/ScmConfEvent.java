package com.sequoiacm.config.framework.event;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class ScmConfEvent {
    private final EventType eventType;
    private NotifyOption notifyOption;
    private String businessType;

    public ScmConfEvent(String businessType, EventType eventType, NotifyOption notifyOption) {
        this.businessType = businessType;
        this.notifyOption = notifyOption;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public NotifyOption getNotifyOption() {
        return notifyOption;
    }

    @Override
    public String toString() {
        return "businessType=" + businessType + ",notifyOption=" + notifyOption;
    }

}
