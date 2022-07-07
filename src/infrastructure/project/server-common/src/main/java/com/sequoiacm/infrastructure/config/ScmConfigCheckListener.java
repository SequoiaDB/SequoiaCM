package com.sequoiacm.infrastructure.config;

import com.sequoiacm.infrastructure.common.CheckRuleUtils;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class ScmConfigCheckListener implements SmartApplicationListener, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event)
                    .getEnvironment();
            //服务启动时，监听服务名是否符合主机名规范
            String applicationName = environment.getProperty("spring.application.name");
            boolean checkResult = CheckRuleUtils.isConformHostNameRule(applicationName);
            if(!checkResult){
                throw new IllegalArgumentException("failed to resolve name:" + applicationName + ",because " + applicationName + " does not conform to host name specification");
            }
        }
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    @Override
    public int getOrder() {
        return ConfigFileApplicationListener.DEFAULT_ORDER + 10;
    }
}
