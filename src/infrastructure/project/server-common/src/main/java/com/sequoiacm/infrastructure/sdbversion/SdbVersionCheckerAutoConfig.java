package com.sequoiacm.infrastructure.sdbversion;

import com.sequoiacm.infrastructure.metasource.config.SequoiadbConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

@Configuration
public class SdbVersionCheckerAutoConfig implements ImportAware {
    private Class<? extends VersionFetcher> versionFetcherClass;

    @Bean
    public SdbVersionChecker sdbVersionChecker(ApplicationContext context) {
        VersionFetcher bean = context.getBean(versionFetcherClass);
        return new SdbVersionChecker(bean);
    }

    @Bean
    public SdbVersionInterceptor sdbVersionInterceptor(Environment env,
            SdbVersionChecker sdbVersionChecker) {
        return new SdbVersionInterceptor(sdbVersionChecker, env);
    }

    @Bean
    public SdbVersionWebMvcConfig sdbVersionWebMvcConfig(SdbVersionInterceptor interceptor) {
        return new SdbVersionWebMvcConfig(interceptor);
    }

    @Bean
    @ConditionalOnMissingBean(SequoiadbConfig.class)
    public SequoiadbConfig sequoiadbConfig() {
        return new SequoiadbConfig();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importMetadata
                .getAnnotationAttributes(EnableSdbVersionChecker.class.getName()));

         versionFetcherClass = attributes
                .getClass("versionFetcher");
    }
}
