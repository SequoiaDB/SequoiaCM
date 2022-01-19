package com.sequoiacm.contentserver.contentmodule;

import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
    public ContentModuleInitRunner contentModuleInitRunner() {
        return new ContentModuleInitRunner();
    }

    @Bean
    public ContentModuleConfig contentModuleConfig() {
        return new ContentModuleConfig();
    }
}

class ContentModuleInitRunner {
    private static final Logger logger = LoggerFactory.getLogger(ContentModuleInitRunner.class);

    @Autowired
    private ScmPrivClient privClient;
    @Autowired
    private ScmConfClient confClient;
    @Autowired
    private Registration localService;
    @Autowired
    private ContentModuleConfig config;

    @PostConstruct
    public void init() throws Exception {
        ContentModuleInitializer initializer = new ContentModuleInitializer(privClient, confClient,
                localService.getServiceId(), config.getSite(), null);
        initializer.initBizComponent();
        String instance = localService.getHost() + localService.getPort();
        int instanceHash = instance.hashCode();
        // id generator 只使用低16位，这里做一次异或扰乱低16位
        short instanceHashShort = (short) ((instanceHash >>> 16) ^ instanceHash);
        logger.info("init id generator: instance={}, serverId(hashCode)={}", instance,
                instanceHashShort);
        initializer.initIdGenerator(instanceHashShort);
    }

    @PreDestroy
    public void destroy() throws ScmServerException {
        ScmLockManager.getInstance().close();
        ScmContentModule.getInstance().getSiteMgr().clear();
    }
}
