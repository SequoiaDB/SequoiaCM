package com.sequoiacm.mq.server;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class MqServer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MqServer.class).bannerMode(Banner.Mode.OFF).run(args);
    }

}
