package com.sequoiacm.infrastructure.remote;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

public interface ConnectivityCheckHealth {
    String S3_HEALTH_PATH = "/internal/v1/health?action=actuator";
    String HEALTH_PATH = "/internal/v1/health";

    // ***************health***************//
    @GetMapping(value = HEALTH_PATH)
    public Map<?, ?> getHealth();

    // ***************S3 health***************//
    @GetMapping(value = S3_HEALTH_PATH)
    public Map<?, ?> getS3Health();

}
