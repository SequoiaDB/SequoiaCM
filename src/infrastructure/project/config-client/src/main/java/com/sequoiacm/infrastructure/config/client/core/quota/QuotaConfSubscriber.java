package com.sequoiacm.infrastructure.config.client.core.quota;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaFilter;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaVersionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuotaConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(QuotaConfSubscriber.class);

    private long heartbeatInterval;
    @Value("${spring.application.name}")
    private String myServiceName;
    private ScmConfClient confClient;

    private VersionFilter versionFilter;

    private Map<String, QuotaConfig> quotaCache = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @PostConstruct
    private void postConstruct() throws ScmConfigException {
        confClient.subscribeWithAsyncRetry(this);
    }

    @Autowired
    public QuotaConfSubscriber(QuotaSubscriberConfig config, ScmConfClient confClient,
            ApplicationContext applicationContext) {
        this.confClient = confClient;
        this.heartbeatInterval = config.getHeartbeatInterval();
        this.versionFilter = new QuotaVersionFilter();
        this.applicationContext = applicationContext;
    }

    public QuotaConfig getQuota(String type, String name) throws ScmConfigException {
        QuotaConfig config = quotaCache.get(QuotaConfig.toBusinessName(type, name));
        if (config != null) {
            return config;
        }
        return refreshQuotaCache(type, name);
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.QUOTA;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive notification:" + notification);
        QuotaNotifyOption option = (QuotaNotifyOption) notification;
        String businessName = option.getBusinessName();
        String type = option.getType();
        String name = option.getName();
        if (notification.getEventType() == EventType.DELTE) {
            quotaCache.remove(businessName);
            applicationContext.publishEvent(new QuotaChangeEvent(type, name, null));
            return;
        }
        QuotaConfig quotaConfig = refreshQuotaCache(option.getType(), option.getName());
        applicationContext.publishEvent(new QuotaChangeEvent(type, name, quotaConfig));
    }

    private QuotaConfig refreshQuotaCache(String type, String name) throws ScmConfigException {
        QuotaConfig quotaConfig = (QuotaConfig) confClient.getOneConf(ScmConfigNameDefine.QUOTA,
                new QuotaFilter(type, name));
        if (quotaConfig != null) {
            quotaCache.put(quotaConfig.getBusinessName(), quotaConfig);
        }
        else {
            quotaCache.remove(QuotaConfig.toBusinessName(type, name));
        }
        return quotaConfig;
    }

    @Override
    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    @Override
    public long getHeartbeatIterval() {
        return this.heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        return new QuotaNotifyOption(version.getBussinessName(), version.getVersion(), eventType);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }
}
