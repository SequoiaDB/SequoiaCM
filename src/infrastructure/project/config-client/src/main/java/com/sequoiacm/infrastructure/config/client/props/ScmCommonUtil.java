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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

@Component
public class ScmCommonUtil {
    private Logger logger = LoggerFactory.getLogger(ScmCommonUtil.class);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private ResourceLoader resourceLoader;

    private String localConfigFilePropertiesSourceName = null;
    private Set<Class<?>> scmConfClasses;

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
        if (scmConfClasses != null) {
            return scmConfClasses;
        }

        Set<Class<?>> ret = new HashSet<>();
        String packagePath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                .concat(ClassUtils.convertClassNameToResourcePath(
                        SystemPropertyUtils.resolvePlaceholders("com.sequoiacm")))
                .concat("/**/*.class");
        ResourcePatternResolver resolver = ResourcePatternUtils
                .getResourcePatternResolver(resourceLoader);
        CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory(resourceLoader);
        Resource[] resources = resolver.getResources(packagePath);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader reader = factory.getMetadataReader(resource);
                if (reader.getClassMetadata().isConcrete()) {
                    String clazzName = reader.getClassMetadata().getClassName();
                    try {
                        Class<?> clazz = Class.forName(clazzName);
                        if (AnnotationUtils.findAnnotation(clazz,
                                ConfigurationProperties.class) != null) {
                            ret.add(clazz);
                            continue;
                        }
                        if (isFieldWithValueAnnotation(clazz)) {
                            ret.add(clazz);
                        }
                    }
                    catch (Throwable e) {
                        // 有些类是无法加载的，因为缺少该类的相关依赖，
                        // 之所以缺少相关依赖是因为这个类本身是不参与服务逻辑的，所以其相关依赖没有纳入；如：ScmUndertowConnectInfoEndpoint
                        logger.debug("ignore the clazz cause by parse exception: " + clazzName, e);
                    }

                }
            }
        }
        scmConfClasses = ret;
        return ret;
    }

    // 判断类的任意成员变量是否被 @Value 注解修饰
    private boolean isFieldWithValueAnnotation(Class<?> clazz) {
        // 除了搜索这个类本身，还会继续搜索其父类，直到搜索到 Object.class
        Class<?> targetClass = ClassUtils.getUserClass(clazz);
        do {
            Field[] fields = targetClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.getAnnotation(Value.class) != null) {
                    return true;
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);
        return false;

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