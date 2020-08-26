package com.sequoiacm.schedule.client;

import org.springframework.context.annotation.Bean;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;

public class ScheduleClientAutoConfig {

    @Bean
    public ScheduleClient schClient(ScmFeignClient feignClient) {
        return new ScheduleClient(feignClient);
    }
}
