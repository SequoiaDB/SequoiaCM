package com.sequoiacm.om.omserver;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ScmOmServer implements ApplicationRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScmOmServer.class).web(true).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
    }
}
