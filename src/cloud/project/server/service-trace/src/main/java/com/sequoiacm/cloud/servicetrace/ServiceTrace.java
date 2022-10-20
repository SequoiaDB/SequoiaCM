package com.sequoiacm.cloud.servicetrace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.verifier.PreventingModificationVerifier;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;

import zipkin.server.EnableZipkinServer;

@EnableDiscoveryClient
@EnableZipkinServer
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
public class ServiceTrace implements ApplicationRunner {

    @Autowired
    ScmConfClient confClient;

    public static void main(String[] args) {
        new SpringApplicationBuilder(ServiceTrace.class).bannerMode(Banner.Mode.OFF).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        confClient.registerConfigPropVerifier(new PreventingModificationVerifier("scm."));
    }

}
