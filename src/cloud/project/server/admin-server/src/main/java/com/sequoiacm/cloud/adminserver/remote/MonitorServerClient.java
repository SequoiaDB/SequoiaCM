package com.sequoiacm.cloud.adminserver.remote;

import java.util.Collection;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.GetMapping;

import com.sequoiacm.cloud.adminserver.model.HealthInfo;
import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

public interface MonitorServerClient {

    // ***************health***************//
    @GetMapping(value = "/health")
    public HealthInfo getHeahth();

    // ***************hostinfo****************//
    @GetMapping("/internal/v1/monitor_collector/host_info")
    public BSONObject getHostInfo();

    // **********contentserver file flow**********//
    @GetMapping("/internal/v1/monitor_collector/show_flow")
    public Collection<WorkspaceFlow> showFlow();

    // ************gateway gauge response**********//
    @GetMapping("/internal/v1/monitor_collector/gauge_response")
    public BSONObject gaugeResponse();
}
