package com.sequoiacm.contentserver.contentmodule;

import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;

@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableAudit
@EnableScmMonitorServer
@EnableScmPrivClient
@EnableDiscoveryClient
@EnableConfClient
@EnableHystrix
@ComponentScan(basePackages = { "com.sequoiacm.contentserver" }, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {
                ContentModuleExcludeMarker.class, Controller.class }) })
public class ContentModuleAutoConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ContentModuleInitRunner contentModuleInitRunner() {
        return new ContentModuleInitRunner();
    }

    @Bean
    public ContentModuleConfig contentModuleConfig() {
        return new ContentModuleConfig();
    }
}

