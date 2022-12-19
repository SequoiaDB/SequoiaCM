package com.sequoiacm.cloud.servicecenter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

import com.sequoiacm.infrastructure.audit.EnableAudit;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.metasource.EnableSdbDataSource;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;

@EnableEurekaServer
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
@EnableSdbDataSource
@EnableScmPrivClient
@EnableAudit
@EnableHystrix
public class ServiceCenter {

    @Autowired
    ScmConfClient confClient;
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(ServiceCenter.class).bannerMode(Banner.Mode.OFF).web(true).run(args);
    }

}
