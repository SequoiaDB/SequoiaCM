package com.sequoiacm.infrastructure.config.client.cache.quota;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaFilter;

public class QuotaConfCache implements NotifyCallback {
    private static final Logger logger = LoggerFactory.getLogger(QuotaConfCache.class);

    @Autowired
    private ScmConfClient confClient;

    private final Map<String, QuotaConfig> quotaCache = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() throws ScmConfigException {
        confClient.subscribe(ScmBusinessTypeDefine.QUOTA, this);
    }

    @Autowired
    public QuotaConfCache(ScmConfClient confClient) {
        this.confClient = confClient;
    }

    public QuotaConfig getQuota(String type, String name) throws ScmConfigException {
        QuotaConfig config = quotaCache.get(QuotaConfig.toBusinessName(type, name));
        if (config != null) {
            return config;
        }
        return refreshQuotaCache(type, name);
    }

    private QuotaConfig refreshQuotaCache(String type, String name) throws ScmConfigException {
        QuotaConfig quotaConfig = (QuotaConfig) confClient.getOneConf(ScmBusinessTypeDefine.QUOTA,
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
    public void processNotify(EventType type, String businessName, NotifyOption notification) {
        if (type == EventType.DELTE) {
            quotaCache.remove(businessName);
            return;
        }

        try {
            refreshQuotaCache(QuotaConfig.getTypeFromBusinessName(businessName),
                    QuotaConfig.getNameFromBusinessName(businessName));
        }
        catch (Exception e) {
            logger.warn("failed to refresh quota cache: type={} businessName={} notification={}",
                    type, businessName, notification, e);
            quotaCache.remove(businessName);
        }

    }

    @Override
    public int priority() {
        return NotifyCallback.HIGHEST_PRECEDENCE;
    }
}
