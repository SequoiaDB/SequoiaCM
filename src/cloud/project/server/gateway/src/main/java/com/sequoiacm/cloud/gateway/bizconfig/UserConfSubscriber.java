package com.sequoiacm.cloud.gateway.bizconfig;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import com.sequoiacm.infrastructure.security.auth.ScmAuthenticationFilter;

@Component
public class UserConfSubscriber implements ScmConfSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(UserConfSubscriber.class);

    @Autowired
    private ScmAuthenticationFilter scmAuthenticationFilter;

    // 过滤器 ScmAuthenticationFilter 可以保证用户缓存是在 60s 之内的数据
    // 所以不需要再通过定时器来防止配置服务的通知丢失问题
    private long heartbeatInterval = -1;

    @Value("${spring.application.name}")
    private String myServiceName;
    private ScmConfClient confClient;

    private DefaultVersionFilter versionFilter;

    public UserConfSubscriber(ScmConfClient confClient) {
        this.confClient = confClient;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.USER);
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
    public void processNotify(NotifyOption notification) {
        logger.info("receive notification:" + notification);
        UserNotifyOption option = (UserNotifyOption) notification;
        // 用户的创建事件可以忽略，本地无相应用户缓存时会网关会找 auth-server 拿
        if (notification.getEventType() == EventType.CREATE) {
            return;
        }
        scmAuthenticationFilter.removeCache(option.getUsername());
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
