package com.sequoiacm.cloud.adminserver.remote;

import org.springframework.web.bind.annotation.GetMapping;

import com.sequoiacm.cloud.adminserver.model.HealthInfo;

public interface MonitorServerClientS3Health extends MonitorServerClient {

    String S3_HEALTH_PATH = "/internal/v1/health?action=actuator";
    String S3_SERVER_NAME = "s3";

    // ***************S3 health***************//
    @GetMapping(value = S3_HEALTH_PATH)
    public HealthInfo getHealth();

}