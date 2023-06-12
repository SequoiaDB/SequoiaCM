package com.sequoiacm.fulltext.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

import com.sequoiacm.content.client.EnableContentserverClient;
import com.sequoiacm.fulltext.server.config.PrivilegeHeartBeatConfig;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.mq.client.EnableScmMqAdmin;
import com.sequoiacm.mq.client.EnableScmMqConsumer;
import com.sequoiacm.mq.client.EnableScmMqProducer;
import com.sequoiacm.schedule.client.EnableScheduleClient;
import com.sequoiacm.schedule.client.EnableScheduleWorker;

@EnableEurekaClient
@SpringBootApplication
@EnableConfClient
@EnableScheduleClient
@EnableScheduleWorker
@EnableScmMqConsumer
@EnableScmMqAdmin
@EnableScmMqProducer
@EnableContentserverClient
@EnableHystrix
public class FulltextServer implements ApplicationRunner {

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private PrivilegeHeartBeatConfig config;

    public static void main(String[] args) {
        new SpringApplicationBuilder(FulltextServer.class).bannerMode(Banner.Mode.OFF).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        privClient.updateHeartbeatInterval(config.getInterval());
    }

}
