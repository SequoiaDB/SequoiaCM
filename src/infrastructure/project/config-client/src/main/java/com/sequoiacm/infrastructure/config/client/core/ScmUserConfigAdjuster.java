package com.sequoiacm.infrastructure.config.client.core;

import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 这个类的作用是在用户启动节点时，将用户配置文件中一些配置进行调整；例如在升级到新版本时，可以通过这个类调整一些用户配置文件中遗留的影响新版本功能的配置。
 */
public class ScmUserConfigAdjuster implements SmartApplicationListener, Ordered {

    private static final List<String> deleteUserConfigKeys = Arrays.asList("spring.zipkin.enabled",
            "spring.zipkin.base-url", "spring.zipkin.baseUrl");

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    // spring boot 环境准备好时（发生在应用启动阶段），调整用户配置文件中的配置
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event)
                    .getEnvironment();
            String configFilePath = environment
                    .getProperty(ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY);
            if (configFilePath == null) {
                return;
            }
            if (deleteUserConfigKeys.size() > 0) {
                try {
                    new ScmConfigPropsDao(configFilePath).modifyPropsFile(new HashMap<>(),
                            deleteUserConfigKeys);
                }
                catch (ScmConfigException e) {
                    throw new RuntimeException("failed to modify config file: path="
                            + configFilePath + ", deletedConfigKeys=" + deleteUserConfigKeys, e);
                }
            }
        }
    }

    // 优先必须最高，否则配置文件已经被解析
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
