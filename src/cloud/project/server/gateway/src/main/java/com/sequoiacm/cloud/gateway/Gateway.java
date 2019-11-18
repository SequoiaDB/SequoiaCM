package com.sequoiacm.cloud.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

import com.sequoiacm.cloud.gateway.config.ApacheHttpClientConfiguration;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;

@EnableZuulProxy
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableScmMonitorServer
@EnableConfClient
@ComponentScan(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApacheHttpClientConfiguration.class))
@RibbonClients(defaultConfiguration = ApacheHttpClientConfiguration.class)
public class Gateway implements ApplicationRunner {
    @Autowired
    private ScmConfClient confClient;

    public static void main(String[] args) {
        new SpringApplicationBuilder(Gateway.class).web(true).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));
    }
}
