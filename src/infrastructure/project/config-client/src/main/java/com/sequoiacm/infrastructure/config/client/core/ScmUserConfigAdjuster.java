package com.sequoiacm.infrastructure.config.client.core;

import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 这个类的作用是在用户启动节点时，将用户配置文件中一些配置进行调整；例如在升级到新版本时，可以通过这个类调整一些用户配置文件中遗留的影响新版本功能的配置。
 */
public class ScmUserConfigAdjuster implements SmartApplicationListener, Ordered {

    private static final DeferredLog logger = new DeferredLog();

    private static final List<String> deleteUserConfigKeys = Arrays.asList("spring.zipkin.enabled",
            "spring.zipkin.base-url", "spring.zipkin.baseUrl");

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
                || ApplicationReadyEvent.class.isAssignableFrom(eventType);
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

            // preprocess for trimming the extra(start/trailing) whitespaces of
            // configuration
            Map<String, String> updateProps = new HashMap<>();
            try {
                Resource fsResource = new FileSystemResource(configFilePath);
                Properties properties = PropertiesLoaderUtils.loadProperties(fsResource);
                for (String propertyName : properties.stringPropertyNames()) {
                    String property = properties.getProperty(propertyName);
                    String trimmedProperty;
                    if (property != null && !property
                            .equals((trimmedProperty = StringUtils.trimToEmpty(property)))) {
                        updateProps.put(propertyName, trimmedProperty);
                    }
                }

                if (!updateProps.isEmpty()) {
                    logger.warn(String.format(
                            "trimming whitespaces at the start and end of configurations in path=%s, updateProps=%s.",
                            configFilePath, updateProps));
                    new ScmConfigPropsDao(configFilePath).modifyPropsFile(updateProps,
                            Collections.emptyList());
                }
            }
            catch (IOException e) {
                throw new RuntimeException(String.format(
                        "failed to load application.properties from path=%s.", configFilePath), e);
            }
            catch (ScmConfigException e) {
                throw new RuntimeException(String.format(
                        "failed to trim whitespaces at start and end of configurations in path=%s, updateProps=%s.",
                        configFilePath, updateProps), e);
            }
        }
        else if (event instanceof ApplicationReadyEvent) {
            logger.replayTo(ScmUserConfigAdjuster.class);
        }
    }

    // 优先必须最高，否则配置文件已经被解析
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
