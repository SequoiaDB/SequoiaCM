package com.sequoiacm.mq.client.config;

import org.springframework.context.annotation.Bean;

public class ConsumerConfig {
    @Bean
    public Marker consumerMarkerBean() {
        return new Marker();
    }

    class Marker {
    }
}
