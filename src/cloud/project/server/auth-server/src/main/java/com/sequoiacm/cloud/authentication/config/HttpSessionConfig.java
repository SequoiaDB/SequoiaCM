package com.sequoiacm.cloud.authentication.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.data.sequoiadb.AbstractSequoiadbSessionConverter;
import org.springframework.session.data.sequoiadb.JacksonSequoiadbSessionConverter;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.session.web.http.HeaderHttpSessionStrategy;
import org.springframework.session.web.http.HttpSessionStrategy;

import com.fasterxml.jackson.databind.Module;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiadb.datasource.SequoiadbDatasource;
@Configuration
public class HttpSessionConfig extends SpringHttpSessionConfiguration {

    @Autowired
    private SessionConfig sessionConfig;

    @Autowired
    private CollectionConfig collectionConfig;

    @Bean
    public AbstractSequoiadbSessionConverter sequoiadbSessionConverter() {
        List<Module> securityModules = SecurityJackson2Modules.getModules(getClass().getClassLoader());
        JacksonSequoiadbSessionConverter converter = new JacksonSequoiadbSessionConverter(securityModules);
        converter.getObjectMapper()
        .addMixIn(ScmUser.class, ScmUserMixin.class)
        .addMixIn(ScmRole.class, ScmRoleMixin.class);

        return converter;
    }

    private static class ScmUserMixin {
        // nothing
    }

    private static class ScmRoleMixin {
        // nothing
    }

    @Bean
    public HttpSessionStrategy httpSessionStrategy() {
        return new HeaderHttpSessionStrategy();
    }

    @Bean
    public SequoiadbSessionRepository sequoiadbSessionRepository(
            SequoiadbDatasource sequoiadbDatasource) {
        SequoiadbSessionRepository repository = new SequoiadbSessionRepository(
                sequoiadbDatasource);
        repository.setMaxInactiveIntervalInSeconds(sessionConfig.getMaxInactiveInterval());
        repository.setSequoiadbSessionConverter(sequoiadbSessionConverter());
        repository.setCollectionSpaceName(collectionConfig.getCollectionSpaceName());
        repository.setCollectionName(collectionConfig.getSessionCollectionName());
        return repository;
    }
}
