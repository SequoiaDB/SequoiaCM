package com.sequoiacm.infrastructure.config.client.props;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

@Component
public class ScmCommonUtil {
    private Logger logger = LoggerFactory.getLogger(ScmCommonUtil.class);

    @Autowired
    private ApplicationContext context;

    private String localConfigFilePropertiesSourceName = null;

    public String getLocalConfigFilePropName() {
        if (localConfigFilePropertiesSourceName != null) {
            return localConfigFilePropertiesSourceName;
        }

        String configFilePath = context.getEnvironment()
                .getProperty(ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY);
        configFilePath = StringUtils.cleanPath(configFilePath);
        if (!ResourceUtils.isUrl(configFilePath)) {
            this.localConfigFilePropertiesSourceName = "applicationConfig: ["
                    + ResourceUtils.FILE_URL_PREFIX + configFilePath + "]";
            return localConfigFilePropertiesSourceName;
        }
        throw new IllegalArgumentException("local config file is unknown: " + configFilePath);
    }

    // 返回 scm 配置类对象：
    // 1. 类上被 @ConfigurationProperties 注解修饰
    // 2. 类的任意成员变量被 @Value 注解修饰
    // 3. 类的包路径以 com.sequoiacm 开头
    public Set<Class<?>> getScmConfClass() throws IOException, ClassNotFoundException {
        return ScmConfClassScanner.getScmConfClasses();
    }

    public Map<String, Object> getRefreshableBeans() {
        return context.getBeansWithAnnotation(RefreshScope.class);
    }

    // 获取 @Value 注解代表的配置项
    public static String getValueConf(Value valueAnnotation) {
        try {
            // value = ${conf:default} or value = ${conf}
            String value = valueAnnotation.value();

            // confColonDefault = conf:default
            String confColonDefault = value.substring(value.indexOf("{") + 1,
                    value.lastIndexOf("}"));
            int colonIdx = confColonDefault.indexOf(":");
            colonIdx = colonIdx == -1 ? confColonDefault.length() : colonIdx;

            return confColonDefault.substring(0, colonIdx);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                    "failed to parse @Value annotation: value=" + valueAnnotation.value(), e);
        }
    }

    public static Object getFieldValue(Field field, Object obj) throws IllegalAccessException {
        Object objValue;
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