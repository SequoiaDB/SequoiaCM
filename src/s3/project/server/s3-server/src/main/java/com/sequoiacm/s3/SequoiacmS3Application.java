package com.sequoiacm.s3;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.sequoiadb.infrastructure.map.client.EnableMapClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

@EnableDiscoveryClient
@EnableMapClient
@SpringBootApplication
@EnableHystrix
public class SequoiacmS3Application implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SequoiacmS3Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

    }
}
