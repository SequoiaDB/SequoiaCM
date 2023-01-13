package com.sequoiacm.infrastructure.config.client.props;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDao;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDaoFactory;

// 监听程序启动、刷新配置事件，触发如下动作：
// 比对每一个 scm config bean 的成员变量值，确认与配置文件是否一致，不一致则回写至配置文件
@Component
public class ScmConfFileRewriteListener {
    private final String localConfigFilePropName;
    private final ScmCommonUtil util;

    @Autowired
    private ApplicationContext context;
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
        catch (Throwable e) {
            logger.warn("failed to rewrite conf file", e);
        }
    }

    private synchronized void onEvent() throws Exception {
        // key=需要重写的配置项，value=配置项的值
        Map<String, String> rewriteConf = new HashMap<>();
        Set<Class<?>> scmConfClass = util.getScmConfClass();
        for (Class<?> clazz : scmConfClass) {
            Object bean;
            try {
                bean = context.getBean(clazz);
                if (bean instanceof Advised) {
                    // 经过代理的对象，我们需要拿到它的原始对象进行解析
                    Object targetBean = ((Advised) bean).getTargetSource().getTarget();
                    if (targetBean == null) {
                        // 接口声明可能会返回 null 表示无法获取原始对象，实测 scm 没有这种情况
                        logger.info("ignore null advised bean: class={}, bean={}", clazz, bean);
                        continue;
                    }
                    bean = targetBean;
                }
            }
            catch (NoSuchBeanDefinitionException e) {
                // 这里会由于 context.getBean(clazz) 找不到对象而抛出异常，找不到对象是因为这个类可能不满足条件注入容器
                logger.debug("failed to parse conf bean for rewrite: " + clazz, e);
                continue;
            }

            ConfigurationProperties propsAnnotation = AnnotationUtils.findAnnotation(clazz,
                    ConfigurationProperties.class);
            if (propsAnnotation != null) {
                // 配置 bean 是 @ConfigurationProperties 注解的
                String confPrefix = propsAnnotation.prefix();
                ScmConfigBeanComparator comparator = new ScmConfigBeanComparator(confPrefix, bean,
                        env, conversionService, localConfigFilePropName);
                rewriteConf.putAll(comparator.compare());
            }
            else {
                // 配置 bean 是有成员变量被 @Value 修饰的
                ScmValueBeanComparator comparator = new ScmValueBeanComparator(bean, env,
                        conversionService);
                rewriteConf.putAll(comparator.compare());
            }
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

// 处理成员变量携带 @value 注解的对象，比对实际值及配置文件值
class ScmValueBeanComparator {
    private static final Logger logger = LoggerFactory.getLogger(ScmValueBeanComparator.class);
    private final Object bean;
    private final ConversionService conversionService;
    private final ConfigurableEnvironment env;

    public ScmValueBeanComparator(Object bean, ConfigurableEnvironment env,
            ConversionService conversionService) {
        this.bean = bean;
        this.env = env;
        this.conversionService = conversionService;
    }

    public Map<String, String> compare() {
        Map<String, String> ret = new HashMap<>();
        Class<?> clazz = ClassUtils.getUserClass(bean.getClass());
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
                Value valueAnnotation = field.getAnnotation(Value.class);
                if (valueAnnotation == null) {
                    return;
                }

                Object beanValue = ScmCommonUtil.getFieldValue(field, bean);
                if (beanValue == null) {
                    return;
                }

                String conf = ScmCommonUtil.getValueConf(valueAnnotation);
                Object confValue = env.getProperty(conf, field.getType());
                if (confValue == null) {
                    return;
                }

                if (!confValue.equals(beanValue)) {
                    ret.put(conf, conversionService.convert(beanValue, String.class));
                }
            }
        }, RewritableFieldFilter.INSTANCE);

        return ret;
    }
}

// 处理 @ConfigurationProperties 注解的对象，比对实际值及配置文件值
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
        Class<?> clazz = ClassUtils.getUserClass(bean.getClass());
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
                    Object beanValue = ScmCommonUtil.getFieldValue(field, bean);
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
                    Map<Object, Object> beanMap = (Map<Object, Object>) ScmCommonUtil
                            .getFieldValue(field, bean);
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
                Object beanValue = ScmCommonUtil.getFieldValue(field, bean);
                if (beanValue != null && isContainConfPrefix(conf + ".")) {
                    ScmConfigBeanComparator comparator = new ScmConfigBeanComparator(conf,
                            beanValue, env, conversionService, localConfigFilePropSourceName);
                    ret.putAll(comparator.compare());
                }
            }
        }, RewritableFieldFilter.INSTANCE);

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
}

class RewritableFieldFilter implements ReflectionUtils.FieldFilter {

    static RewritableFieldFilter INSTANCE = new RewritableFieldFilter();

    @Override
    public boolean matches(Field field) {
        return field.getAnnotation(ScmRewritableConfMarker.class) != null;
    }
}
