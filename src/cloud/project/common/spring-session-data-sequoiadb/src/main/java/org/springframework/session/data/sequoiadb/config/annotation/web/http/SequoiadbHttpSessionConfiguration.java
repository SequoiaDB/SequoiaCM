/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.session.data.sequoiadb.config.annotation.web.http;

import com.sequoiadb.base.SequoiadbDatasource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.data.sequoiadb.AbstractSequoiadbSessionConverter;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Configuration class registering {@code SequoiadbSessionRepository} bean. To import this
 * configuration use {@link EnableSequoiadbHttpSession} annotation.
 */
@Configuration
public class SequoiadbHttpSessionConfiguration extends SpringHttpSessionConfiguration
        implements EmbeddedValueResolverAware, ImportAware {

    private AbstractSequoiadbSessionConverter sequoiadbSessionConverter;

    private Integer maxInactiveIntervalInSeconds;
    private String collectionSpaceName;
    private String collectionName;

    private StringValueResolver embeddedValueResolver;

    @Bean
    public SequoiadbSessionRepository sequoiadbSessionRepository(
            SequoiadbDatasource sequoiadbDatasource) {
        SequoiadbSessionRepository repository = new SequoiadbSessionRepository(
                sequoiadbDatasource);
        repository.setMaxInactiveIntervalInSeconds(this.maxInactiveIntervalInSeconds);
        if (this.sequoiadbSessionConverter != null) {
            repository.setSequoiadbSessionConverter(this.sequoiadbSessionConverter);
        }
        if (StringUtils.hasText(this.collectionSpaceName)) {
            repository.setCollectionSpaceName(this.collectionSpaceName);
        }
        if (StringUtils.hasText(this.collectionName)) {
            repository.setCollectionName(this.collectionName);
        }
        return repository;
    }

    public void setCollectionSpaceName(String collectionSpaceName) {
        this.collectionSpaceName = collectionSpaceName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
        this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
    }

    public void setImportMetadata(AnnotationMetadata importMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importMetadata
                .getAnnotationAttributes(EnableSequoiadbHttpSession.class.getName()));

        this.maxInactiveIntervalInSeconds = attributes
                .getNumber("maxInactiveIntervalInSeconds");

        String collectionSpaceNameValue = attributes.getString("collectionSpaceName");
        if (StringUtils.hasText(collectionSpaceNameValue)) {
            this.collectionSpaceName = this.embeddedValueResolver.resolveStringValue(collectionSpaceNameValue);
        }

        String collectionNameValue = attributes.getString("collectionName");
        if (StringUtils.hasText(collectionNameValue)) {
            this.collectionName = this.embeddedValueResolver.resolveStringValue(collectionNameValue);
        }
    }

    @Autowired(required = false)
    public void setSequoiadbSessionConverter(
            AbstractSequoiadbSessionConverter sequoiadbSessionConverter) {
        this.sequoiadbSessionConverter = sequoiadbSessionConverter;
    }

    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

}
