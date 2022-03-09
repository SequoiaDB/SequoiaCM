package com.sequoiacm.cloud.servicecenter;

import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.metasource.EnableSdbDataSource;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

@EnableEurekaServer
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
@EnableSdbDataSource
@EnableScmPrivClient
@EnableAudit
@EnableHystrix
public class ServiceCenter implements ApplicationRunner {

    @Autowired
    ScmConfClient confClient;
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(ServiceCenter.class).web(true).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));
    }
}
