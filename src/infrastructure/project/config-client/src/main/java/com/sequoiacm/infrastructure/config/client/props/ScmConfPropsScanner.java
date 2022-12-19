package com.sequoiacm.infrastructure.config.client.props;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;
import com.sequoiacm.infrastructure.common.annotation.ScmUnRefreshableConfigMarker;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ScmConfPropsScanner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfPropsScanner.class);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ScmCommonUtil util;

    // 支持动态刷新的scm配置列表
    private final Set<ScmPropsMatchRule> refreshableConfRules = new HashSet<>();

    // scm配置列表
    private final Set<ScmPropsMatchRule> scmConfRules = new HashSet<>();

    // scm配置与类型的映射（ scm.test.intConf = Integer.class）
    private final Map<ScmPropsMatchRule, Class<?>> confRulMapType = new HashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Object> scmConfPropsClasses = util.getScmConfBeans();
        for (Map.Entry<String, Object> entry : scmConfPropsClasses.entrySet()) {
            boolean isRefreshScopeBean = context.findAnnotationOnBean(entry.getKey(),
                    RefreshScope.class) != null;
            Class<?> clazz = ClassUtils.getUserClass(entry.getValue().getClass());
            ConfigurationProperties configAnnotation = AnnotationUtils.findAnnotation(clazz,
                    ConfigurationProperties.class);
            String configPrefix = configAnnotation.prefix();
            if (!configPrefix.endsWith(".")) {
                configPrefix = configPrefix + ".";
            }
            ScmConfFieldCallback fieldCallback = new ScmConfFieldCallback(isRefreshScopeBean,
                    configPrefix, scmConfRules, refreshableConfRules, confRulMapType);
            ReflectionUtils.doWithFields(clazz, fieldCallback, new ReflectionUtils.FieldFilter() {
                @Override
                public boolean matches(Field field) {
                    return true;
                }
            });
        }
        logger.info("scm conf match rules: {}", scmConfRules);
        logger.info("scm refreshable conf match rules: {}", refreshableConfRules);
    }

    public boolean isScmConfProp(String confProp) {
        for (ScmPropsMatchRule rule : scmConfRules) {
            if (rule.isMatch(confProp)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRefreshableConfProp(String confProp) {
        for (ScmPropsMatchRule rule : refreshableConfRules) {
            if (rule.isMatch(confProp)) {
                return true;
            }
        }
        return false;
    }

    public Class<?> getType(String confProp) {
        for (Map.Entry<ScmPropsMatchRule, Class<?>> entry : confRulMapType.entrySet()) {
            if (entry.getKey().isMatch(confProp)) {
                return entry.getValue();
            }
        }
        return null;
    }

}

class ScmConfFieldCallback implements ReflectionUtils.FieldCallback {
    private final String configPrefix;
    private final Set<ScmPropsMatchRule> scmConfRules;
    private final Set<ScmPropsMatchRule> refreshableConfRule;
    private final Map<ScmPropsMatchRule, Class<?>> confRulMapType;
    private final boolean isFieldInRefreshableClass;

    public ScmConfFieldCallback(boolean isFieldInRefreshableClass, String configPrefix,
            Set<ScmPropsMatchRule> scmConfRules, Set<ScmPropsMatchRule> refreshableConfRule,
            Map<ScmPropsMatchRule, Class<?>> confRulMapType) {
        this.configPrefix = configPrefix;
        this.scmConfRules = scmConfRules;
        this.refreshableConfRule = refreshableConfRule;
        this.confRulMapType = confRulMapType;
        this.isFieldInRefreshableClass = isFieldInRefreshableClass;
    }

    private boolean doWithSimpleOrMapType(String prefix, Field field, boolean isRefreshableField) {
        if (DataTypeUtil.isSimpleDataType(field.getType())) {
            ScmPropsExactMatchRule rule = new ScmPropsExactMatchRule(prefix + field.getName());
            scmConfRules.add(rule);
            if (isRefreshableField) {
                refreshableConfRule.add(rule);
            }
            confRulMapType.put(rule, field.getType());
            return true;
        }

        if (DataTypeUtil.isMap(field.getType())) {
            ParameterizedType paramType = (ParameterizedType) field.getGenericType();
            ScmPropsPrefixMatchRule rule = new ScmPropsPrefixMatchRule(
                    prefix + field.getName() + ".");
            scmConfRules.add(rule);
            if (isRefreshableField) {
                refreshableConfRule.add(rule);
            }
            confRulMapType.put(rule, (Class<?>) paramType.getActualTypeArguments()[1]);
            return true;
        }
        return false;
    }

    @Override
    public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
        // 先出识别处理简单数据类型 or map 数据类型
        boolean isRefreshableField = field.getAnnotation(ScmRefreshableConfigMarker.class) != null
                && isFieldInRefreshableClass;
        boolean isProcess = doWithSimpleOrMapType(configPrefix, field, isRefreshableField);
        if (isProcess) {
            return;
        }

        // 按嵌套类型进行处理
        String nestedConfigPrefix = configPrefix + field.getName() + ".";
        ReflectionUtils.doWithFields(field.getType(), new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field nestedField) throws IllegalArgumentException {
                boolean nestedFieldIsRefreshable = isRefreshableField
                        // 嵌套类型内的 field 携带 ScmUnRefreshableConfigMarker 视为不能动态生效
                        && nestedField.getAnnotation(ScmUnRefreshableConfigMarker.class) == null;
                boolean isProcessNestedField = doWithSimpleOrMapType(nestedConfigPrefix,
                        nestedField, nestedFieldIsRefreshable);
                if (isProcessNestedField) {
                    return;
                }

                // 嵌套中的嵌套类结构不再解析，直接按前缀匹配，同时不在解析其配置类型加入 confRulMapType
                ScmPropsPrefixMatchRule rule = new ScmPropsPrefixMatchRule(
                        nestedConfigPrefix + nestedField.getName() + ".");
                scmConfRules.add(rule);
                if (isRefreshableField) {
                    refreshableConfRule.add(rule);
                }
            }
        }, new ReflectionUtils.FieldFilter() {
            @Override
            public boolean matches(Field field) {
                return field.getAnnotation(ScmUnRefreshableConfigMarker.class) == null;
            }
        });
    }
}

interface ScmPropsMatchRule {
    boolean isMatch(String confProp);
}

class ScmPropsPrefixMatchRule implements ScmPropsMatchRule {
    private final String prefix;

    public ScmPropsPrefixMatchRule(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean isMatch(String confProp) {
        return confProp.startsWith(prefix);
    }

    @Override
    public String toString() {
        return prefix + "*";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmPropsPrefixMatchRule that = (ScmPropsPrefixMatchRule) o;
        return Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }
}

class ScmPropsExactMatchRule implements ScmPropsMatchRule {
    private final String conf;

    public ScmPropsExactMatchRule(String conf) {
        this.conf = conf;
    }

    @Override
    public boolean isMatch(String confProp) {
        return conf.equals(confProp);
    }

    @Override
    public String toString() {
        return conf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmPropsExactMatchRule that = (ScmPropsExactMatchRule) o;
        return Objects.equals(conf, that.conf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conf);
    }
}

class DataTypeUtil {

    public static boolean isSimpleDataType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Character.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Number.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (String.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (clazz.isEnum()) {
            return true;
        }
        return false;
    }

    public static boolean isMap(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }
}
