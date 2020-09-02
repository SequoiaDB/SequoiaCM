package com.sequoiadb.infrastructure.map.client;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.sequoiadb.infrastructure.map.ScmMapServerException;

@EnableDiscoveryClient
@SpringBootApplication
@EnableMapClient
public class App {
    public static void main(String[] args) throws ScmMapServerException {
        new SpringApplicationBuilder(App.class).bannerMode(Banner.Mode.CONSOLE).run(args);

    }
}
