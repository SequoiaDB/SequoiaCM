package com.sequoiacm.infrastructure.config.client.props;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ScmConfClassScanner implements SmartApplicationListener {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfClassScanner.class);
    public static Set<Class<?>> scmConfClasses = Collections.emptySet();

    public static Set<Class<?>> getScmConfClasses() {
        return scmConfClasses;
    }

    // 返回 scm 配置类对象：
    // 1. 类上被 @ConfigurationProperties 注解修饰
    // 2. 类的任意成员变量被 @Value 注解修饰
    // 3. 类的包路径以 com.sequoiacm 开头
    private void scan() throws IOException {
        if (!scmConfClasses.isEmpty()) {
            return;
        }
        scmConfClasses = new HashSet<>();
        String packagePath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                .concat(ClassUtils.convertClassNameToResourcePath(
                        SystemPropertyUtils.resolvePlaceholders("com.sequoiacm")))
                .concat("/**/*.class");
        ResourcePatternResolver resolver = ResourcePatternUtils
                .getResourcePatternResolver(null);
        CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
        Resource[] resources = resolver.getResources(packagePath);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader reader = factory.getMetadataReader(resource);
                if (reader.getClassMetadata().isConcrete()) {
                    String clazzName = reader.getClassMetadata().getClassName();
                    try {
                        logger.debug("loading class: {}", clazzName);
                        Class<?> clazz = Class.forName(clazzName);
                        if (AnnotationUtils.findAnnotation(clazz,
                                ConfigurationProperties.class) != null) {
                            scmConfClasses.add(clazz);
                            continue;
                        }
                        if (isFieldWithValueAnnotation(clazz)) {
                            scmConfClasses.add(clazz);
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
            ApplicationEnvironmentPreparedEvent e = (ApplicationEnvironmentPreparedEvent) event;
            // don't listen to events in a bootstrap context
            if ((e.getEnvironment().getPropertySources()
                    .contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME))) {
                return;
            }
            logger.info("scanning scm conf class..");
            try {
                scan();
            }
            catch (IOException ex) {
                throw new RuntimeException("failed to scan scm conf class", ex);
            }
        }
    }

    // 需要在 SlowLogManagerInitializer 之后执行，否则这里会将原始class载入jvm，导致无法进行slowlog的class修改
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
