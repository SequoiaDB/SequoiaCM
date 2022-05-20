package com.sequoiacm.cloud.adminserver.remote;

import org.springframework.web.bind.annotation.GetMapping;
import com.sequoiacm.cloud.adminserver.model.HealthInfo;

public interface MonitorServerClientInternalHealth extends MonitorServerClient {

    // ***************health***************//
    @GetMapping(value = "/internal/v1/health")
    public HealthInfo getHealth();

}
