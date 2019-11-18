package com.sequoiacm.perf;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class PerfApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PerfApplication.class)
                .bannerMode(Banner.Mode.OFF).run(args);
    }
}
