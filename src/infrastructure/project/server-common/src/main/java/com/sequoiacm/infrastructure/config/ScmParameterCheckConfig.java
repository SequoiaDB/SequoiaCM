package com.sequoiacm.infrastructure.config;

import com.sequoiacm.infrastructure.common.CheckParameterResult;
import com.sequoiacm.infrastructure.common.ScmParameterCheckEnum;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public class ScmParameterCheckConfig implements SmartApplicationListener, Ordered {
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event)
                    .getEnvironment();
            ScmParameterCheckEnum[] checkEnums = ScmParameterCheckEnum.values();
            for (ScmParameterCheckEnum checkEnum : checkEnums) {
                String key = checkEnum.getKey();
                if (environment.containsProperty(key)) {
                    String value = environment.getProperty(key);
                    CheckParameterResult checkResult = checkEnum.doValidate(key, value);
                    if (!checkResult.isSuccessful()) {
                        throw new IllegalArgumentException(checkResult.getMsg());
                    }
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return ConfigFileApplicationListener.DEFAULT_ORDER + 10;
    }
}