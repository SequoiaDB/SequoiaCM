package com.sequoiacm.mq.client.config;

import org.springframework.context.annotation.Bean;

public class ProducerConfig {
    @Bean
    public Marker producerMarkerBean() {
        return new Marker();
    }

    class Marker {
    }
}
