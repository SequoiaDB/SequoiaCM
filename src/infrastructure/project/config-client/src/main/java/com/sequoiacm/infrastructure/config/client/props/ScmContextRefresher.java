package com.sequoiacm.infrastructure.config.client.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.StandardServletEnvironment;

// 代码逻辑基本上与 org.springframework.cloud.context.refresh.ContextRefresher 一致，用于替换该
// bean
// 主要调整为刷新配置时，控制只允许 scm 动态刷新配置，及非 scm 配置刷入内存
@Component
public class ScmContextRefresher extends ContextRefresher {
    private static final Logger logger = LoggerFactory.getLogger(ScmContextRefresher.class);
    private String localConfigFilePropertiesSourceName;
    @Autowired
    private ScmConfPropsScanner scmConfPropsScanner;

    private static final String REFRESH_ARGS_PROPERTY_SOURCE = "refreshArgs";

    private static final String[] DEFAULT_PROPERTY_SOURCES = new String[] { // order
            // matters,
            // cli args
            // aren't
            // first,
            // things get
            // messy
            CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME, "defaultProperties" };

    private Set<String> standardSources = new HashSet<>(
            Arrays.asList(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
                    StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
                    StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));

    private ConfigurableApplicationContext context;
    private RefreshScope scope;

    @Autowired
    public ScmContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
            ScmCommonUtil commonUtil) {
        super(context, scope);
        this.context = context;
        this.scope = scope;
        this.localConfigFilePropertiesSourceName = commonUtil.getLocalConfigFilePropName();
        logger.info("scm context refresher is enabled.");
    }

    public synchronized Set<String> refresh() {
        Map<String, Object> before = extract(this.context.getEnvironment().getPropertySources());
        addConfigFilesToEnvironment();
        Set<String> keys = changes(before,
                extract(this.context.getEnvironment().getPropertySources())).keySet();
        this.context.publishEvent(new EnvironmentChangeEvent(context, keys));
        this.scope.refreshAll();
        return keys;
    }

    /* for testing */ ConfigurableApplicationContext addConfigFilesToEnvironment() {
        ConfigurableApplicationContext capture = null;
        try {
            StandardEnvironment environment = copyEnvironment(this.context.getEnvironment());
            SpringApplicationBuilder builder = new SpringApplicationBuilder(
                    org.springframework.cloud.context.refresh.ContextRefresher.Empty.class)
                            .bannerMode(Banner.Mode.OFF).web(false).environment(environment);
            // Just the listeners that affect the environment (e.g. excluding logging
            // listener because it has side effects)
            builder.application().setListeners(Arrays.asList(new BootstrapApplicationListener(),
                    new ConfigFileApplicationListener()));
            capture = builder.run();
            if (environment.getPropertySources().contains(REFRESH_ARGS_PROPERTY_SOURCE)) {
                environment.getPropertySources().remove(REFRESH_ARGS_PROPERTY_SOURCE);
            }
            MutablePropertySources target = this.context.getEnvironment().getPropertySources();
            String targetName = null;
            for (PropertySource<?> source : environment.getPropertySources()) {
                String name = source.getName();
                if (target.contains(name)) {
                    targetName = name;
                }
                if (!this.standardSources.contains(name)) {
                    if (target.contains(name)) {
                        if (name.equals(localConfigFilePropertiesSourceName)) {
                            // SCM 自定义逻辑：控制只允许 scm 动态刷新配置，及非 scm 配置刷入内存
                            source = merge((PropertiesPropertySource) source,
                                    (PropertiesPropertySource) target.get(name));
                        }
                        target.replace(name, source);
                    }
                    else {
                        if (targetName != null) {
                            target.addAfter(targetName, source);
                        }
                        else {
                            // targetName was null so we are at the start of the list
                            target.addFirst(source);
                            targetName = name;
                        }
                    }
                }
            }
        }
        finally {
            ConfigurableApplicationContext closeable = capture;
            while (closeable != null) {
                try {
                    closeable.close();
                }
                catch (Exception e) {
                    // Ignore;
                }
                if (closeable.getParent() instanceof ConfigurableApplicationContext) {
                    closeable = (ConfigurableApplicationContext) closeable.getParent();
                }
                else {
                    break;
                }
            }
        }
        return capture;
    }

    private PropertiesPropertySource merge(PropertiesPropertySource newProps,
            PropertiesPropertySource oldProps) {
        Properties mergeProps = new Properties();

        // 遍历新加载的配置文件，符合条件的刷入内存中
        for (Map.Entry<String, Object> entry : newProps.getSource().entrySet()) {
            if (scmConfPropsScanner.isRefreshableConfProp(entry.getKey())) {
                // 支持在线刷新的配置，需要将新配置刷入内存
                mergeProps.put(entry.getKey(), entry.getValue());
            }
            else if (!scmConfPropsScanner.isScmConfProp(entry.getKey())) {
                // 非SCM定义的配置，需要刷入内存
                mergeProps.put(entry.getKey(), entry.getValue());
            }
            else {
                // SCM 不支持在线刷新的配置，继续沿用旧的配置
                Object oldValue = oldProps.getProperty(entry.getKey());
                if (oldValue != null) {
                    mergeProps.put(entry.getKey(), oldValue);
                }
            }
        }

        // 遍历旧的配置文件，检查在新配置文件被删除的配置项
        for (Map.Entry<String, Object> entry : oldProps.getSource().entrySet()) {
            // 这个配置项在新配置文件被删除了
            if (!mergeProps.contains(entry.getKey())) {
                // 删除的配置项是在线生效的，内存中也删除
                if (scmConfPropsScanner.isRefreshableConfProp(entry.getKey())) {
                    continue;
                }
                // 删除的配置项不是scm配置，内存中也删除
                if (!scmConfPropsScanner.isScmConfProp(entry.getKey())) {
                    continue;
                }
                // 删除的配置项是 SCM 不支持在线刷新的配置，内存中不删除
                mergeProps.put(entry.getKey(), entry.getValue());
            }
        }

        return new PropertiesPropertySource(newProps.getName(), mergeProps);
    }

    // Don't use ConfigurableEnvironment.merge() in case there are clashes with
    // property
    // source names
    private StandardEnvironment copyEnvironment(ConfigurableEnvironment input) {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources capturedPropertySources = environment.getPropertySources();
        // Only copy the default property source(s) and the profiles over from the main
        // environment (everything else should be pristine, just like it was on
        // startup).
        for (String name : DEFAULT_PROPERTY_SOURCES) {
            if (input.getPropertySources().contains(name)) {
                if (capturedPropertySources.contains(name)) {
                    capturedPropertySources.replace(name, input.getPropertySources().get(name));
                }
                else {
                    capturedPropertySources.addLast(input.getPropertySources().get(name));
                }
            }
        }
        environment.setActiveProfiles(input.getActiveProfiles());
        environment.setDefaultProfiles(input.getDefaultProfiles());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("spring.jmx.enabled", false);
        map.put("spring.main.sources", "");
        capturedPropertySources.addFirst(new MapPropertySource(REFRESH_ARGS_PROPERTY_SOURCE, map));
        return environment;
    }

    private Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (String key : before.keySet()) {
            if (!after.containsKey(key)) {
                result.put(key, null);
            }
            else if (!equal(before.get(key), after.get(key))) {
                result.put(key, after.get(key));
            }
        }
        for (String key : after.keySet()) {
            if (!before.containsKey(key)) {
                result.put(key, after.get(key));
            }
        }
        return result;
    }

    private boolean equal(Object one, Object two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        return one.equals(two);
    }

    private Map<String, Object> extract(MutablePropertySources propertySources) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
        for (PropertySource<?> source : propertySources) {
            sources.add(0, source);
        }
        for (PropertySource<?> source : sources) {
            if (!this.standardSources.contains(source.getName())) {
                extract(source, result);
            }
        }
        return result;
    }

    private void extract(PropertySource<?> parent, Map<String, Object> result) {
        if (parent instanceof CompositePropertySource) {
            try {
                List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
                for (PropertySource<?> source : ((CompositePropertySource) parent)
                        .getPropertySources()) {
                    sources.add(0, source);
                }
                for (PropertySource<?> source : sources) {
                    extract(source, result);
                }
            }
            catch (Exception e) {
                return;
            }
        }
        else if (parent instanceof EnumerablePropertySource) {
            for (String key : ((EnumerablePropertySource<?>) parent).getPropertyNames()) {
                result.put(key, parent.getProperty(key));
            }
        }
    }

    @Configuration
    protected static class Empty {

    }

}
