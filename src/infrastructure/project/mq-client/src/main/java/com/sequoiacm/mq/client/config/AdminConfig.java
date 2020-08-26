package com.sequoiacm.mq.client.config;

import org.springframework.context.annotation.Bean;

public class AdminConfig {
    @Bean
    public Marker adminMarkerBean() {
        return new Marker();
    }

    class Marker {
    }
}
