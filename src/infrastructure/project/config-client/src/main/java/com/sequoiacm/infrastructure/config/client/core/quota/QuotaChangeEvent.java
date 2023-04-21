package com.sequoiacm.infrastructure.config.client.core.quota;

import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import org.springframework.context.ApplicationEvent;

public class QuotaChangeEvent extends ApplicationEvent {
    private String type;
    private String name;
    private QuotaConfig newQuotaConfig;

    public QuotaChangeEvent(String type, String name, QuotaConfig newQuotaConfig) {
        super(type + ":" + name);
        this.type = type;
        this.name = name;
        this.newQuotaConfig = newQuotaConfig;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public QuotaConfig getNewQuotaConfig() {
        return newQuotaConfig;
    }
}
