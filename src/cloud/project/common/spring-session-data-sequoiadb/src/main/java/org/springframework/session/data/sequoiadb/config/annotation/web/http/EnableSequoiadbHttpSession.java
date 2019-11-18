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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.sequoiadb.SequoiadbSession;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;

import java.lang.annotation.*;

/**
 * Add this annotation to a {@code @Configuration} class to expose the
 * SessionRepositoryFilter as a bean named "springSessionRepositoryFilter" and backed by
 * SequoiaDB. Use {@code collectionSpaceName} and {@code collectionName} to change default name of the collection space and collection used to
 * store sessions. <pre>
 * <code>
 * {@literal @EnableSequoiadbHttpSession}
 * public class SequoiadbHttpSessionConfig {
 * <p>
 *     {@literal @Bean}
 *     public SequoiadbDatasource sequoiadbDatasource() {
 *         return new SequoiadbDatasource("localhost:11810", "user", "password");
 *     }
 * <p>
 * }
 * </code> </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(SequoiadbHttpSessionConfiguration.class)
@Configuration
public @interface EnableSequoiadbHttpSession {

    /**
     * The maximum time a session will be kept if it is inactive.
     *
     * @return default max inactive interval in seconds
     */
    int maxInactiveIntervalInSeconds() default SequoiadbSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    /**
     * The collection space name to use.
     *
     * @return name of the collection space to store session
     */
    String collectionSpaceName() default SequoiadbSessionRepository.DEFAULT_COLLECTION_SPACE_NAME;

    /**
     * The collection name to use.
     *
     * @return name of the collection to store session
     */
    String collectionName() default SequoiadbSessionRepository.DEFAULT_COLLECTION_NAME;
}
