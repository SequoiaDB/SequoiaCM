package com.sequoiacm.infrastructure.config.client.props;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDao;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDaoFactory;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

// 监听程序启动、刷新配置事件，触发如下动作：
// 比对每一个 scm config bean 的成员变量值，确认与配置文件是否一致，不一致则回写至配置文件
@Component
public class ScmConfFileRewriteListener {
    private final String localConfigFilePropName;
    private final ScmCommonUtil util;

    @Autowired
    private ConfigurableEnvironment env;
    @Autowired
    private ScmConfigPropsDaoFactory confDaoFactory;
    @Autowired
    private ConversionService conversionService;
    private static final Logger logger = LoggerFactory.getLogger(ScmConfFileRewriteListener.class);

    public ScmConfFileRewriteListener(ScmCommonUtil util) {
        localConfigFilePropName = util.getLocalConfigFilePropName();
        this.util = util;
    }

    @EventListener
    public void onReadyEvent(ApplicationReadyEvent event) {
        onEventSilence();
    }

    @EventListener
    public void onRefreshEvent(RefreshScopeRefreshedEvent event) {
        onEventSilence();
    }

    private void onEventSilence() {
        try {
            onEvent();
        }
        catch (Exception e) {
            logger.warn("failed to rewrite conf file", e);
        }
    }

    private synchronized void onEvent() throws ScmConfigException {
        // key=需要重写的配置项，value=配置项的值
        Map<String, String> rewriteConf = new HashMap<>();
        Map<String, Object> beans = util.getScmConfBeans();
        for (Object bean : beans.values()) {
            ConfigurationProperties propsAnnotation = AnnotationUtils
                    .findAnnotation(bean.getClass(), ConfigurationProperties.class);
            String confPrefix = propsAnnotation.prefix();
            ScmConfigBeanComparator comparator = new ScmConfigBeanComparator(confPrefix, bean, env,
                    conversionService, localConfigFilePropName);
            rewriteConf.putAll(comparator.compare());
        }
        if (rewriteConf.isEmpty()) {
            return;
        }

        logger.info("rewrite conf: {}", rewriteConf);
        ScmConfigPropsDao confDao = confDaoFactory.createConfigPropsDao();
        try {
            confDao.modifyPropsFile(rewriteConf, Collections.emptyList());
        }
        catch (Exception e) {
            confDao.rollbackSilence();
            throw e;
        }
        resetEnv(rewriteConf);
    }

    private void resetEnv(Map<String, String> rewriteConf) {
        PropertiesPropertySource props = (PropertiesPropertySource) env.getPropertySources()
                .get(localConfigFilePropName);
        if (props == null) {
            return;
        }

        Properties newProps = new Properties();
        newProps.putAll(props.getSource());
        newProps.putAll(rewriteConf);

        PropertiesPropertySource newSource = new PropertiesPropertySource(props.getName(),
                newProps);
        env.getPropertySources().replace(props.getName(), newSource);
    }

}

class ScmConfigBeanComparator {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigBeanComparator.class);
    private final String localConfigFilePropSourceName;
    private String confPrefix;
    private final Object bean;
    private final ConversionService conversionService;
    private final ConfigurableEnvironment env;

    public ScmConfigBeanComparator(String confPrefix, Object bean, ConfigurableEnvironment env,
            ConversionService conversionService, String localConfigFilePropSourceName) {
        this.confPrefix = confPrefix;
        if (!confPrefix.equals(".")) {
            this.confPrefix = confPrefix + ".";
        }
        this.bean = bean;
        this.env = env;
        this.conversionService = conversionService;
        this.localConfigFilePropSourceName = localConfigFilePropSourceName;
    }

    public Map<String, String> compare() {
        Map<String, String> ret = new HashMap<>();
        Class<?> clazz = bean.getClass();
        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) {
                try {
                    process(field);
                }
                catch (Exception e) {
                    logger.warn("failed to analyze the field: clazz={}, field={}", clazz.getName(),
                            field.getName(), e);
                }
            }

            private void process(Field field) throws IllegalAccessException {
                // conf = @ConfigurationProperties(prefix) 中的 prefix + field 变量名
                String conf = confPrefix + field.getName();
                if (DataTypeUtil.isSimpleDataType(field.getType())) {
                    // 简单数据类型，直接和配置文件比较
                    Object beanValue = getFieldValue(field, bean);
                    if (beanValue == null) {
                        return;
                    }
                    Object confValue = env.getProperty(conf, field.getType());
                    if (confValue == null) {
                        return;
                    }
                    if (!confValue.equals(beanValue)) {
                        ret.put(conf, conversionService.convert(beanValue, String.class));
                    }
                    return;
                }

                if (DataTypeUtil.isMap(field.getType())) {
                    Map<Object, Object> beanMap = (Map<Object, Object>) getFieldValue(field, bean);
                    if (beanMap == null) {
                        return;
                    }
                    // map 数据类型，如 map{key1=value1, key2=value2}
                    // 检查配置文件的 conf.key1、conf.key2 的值
                    for (Map.Entry<Object, Object> entry : beanMap.entrySet()) {
                        String mapConf = conf + "."
                                + conversionService.convert(entry.getKey(), String.class);
                        Object confValue = env.getProperty(mapConf, entry.getValue().getClass());
                        if (confValue == null) {
                            return;
                        }
                        if (!confValue.equals(entry.getValue())) {
                            ret.put(mapConf,
                                    conversionService.convert(entry.getValue(), String.class));
                        }
                    }
                    return;
                }

                // 嵌套数据类型，继续构造一个新的 ScmConfigBeanComparator 来解析
                Object beanValue = getFieldValue(field, bean);
                if (beanValue != null && isContainConfPrefix(conf + ".")) {
                    ScmConfigBeanComparator comparator = new ScmConfigBeanComparator(conf,
                            beanValue, env, conversionService, localConfigFilePropSourceName);
                    ret.putAll(comparator.compare());
                }
            }
        });

        return ret;
    }

    private boolean isContainConfPrefix(String confPrefix) {
        PropertiesPropertySource props = (PropertiesPropertySource) env.getPropertySources()
                .get(localConfigFilePropSourceName);
        if (props == null) {
            return false;
        }
        Map<String, Object> confMap = props.getSource();
        for (String conf : confMap.keySet()) {
            if (conf.startsWith(confPrefix)) {
                return true;
            }
        }
        return false;
    }

    private Object getFieldValue(Field field, Object obj) throws IllegalAccessException {
        Object objValue = null;
        boolean accessible = field.isAccessible();
        if (!accessible) {
            field.setAccessible(true);
            objValue = field.get(obj);
            field.setAccessible(false);
        }
        else {
            objValue = field.get(obj);
        }
        return objValue;
    }
}
