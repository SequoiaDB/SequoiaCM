package com.sequoiacm.om.omserver;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ServletComponentScan
@EnableAsync
public class ScmOmServer implements ApplicationRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScmOmServer.class).bannerMode(Banner.Mode.OFF).web(true).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
    }
}
