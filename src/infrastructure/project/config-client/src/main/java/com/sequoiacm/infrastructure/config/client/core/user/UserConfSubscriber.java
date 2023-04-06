package com.sequoiacm.infrastructure.config.client.core.user;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.user.UserNotifyOption;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

@EnableScmPrivClient
public class UserConfSubscriber implements ScmConfSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(UserConfSubscriber.class);
    private long heartbeatInterval;
    @Value("${spring.application.name}")
    private String myServiceName;
    private ScmConfClient confClient;

    private DefaultVersionFilter versionFilter;

    @Autowired
    private ScmPrivClient privClient;

    public UserConfSubscriber(UserConfSubscriberConfig config, ScmConfClient confClient) {
        this.confClient = confClient;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.USER);
        this.heartbeatInterval = config.getHeartbeatInterval();
    }

    @PostConstruct
    public void postConstruct() throws ScmConfigException {
        confClient.subscribeWithAsyncRetry(this);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.USER;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive user notification: {}", notification);
        privClient.loadAuth();
    }

    @Override
    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    @Override
    public long getHeartbeatIterval() {
        return heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        return new UserNotifyOption(version.getBussinessName(), eventType);
    }

}
