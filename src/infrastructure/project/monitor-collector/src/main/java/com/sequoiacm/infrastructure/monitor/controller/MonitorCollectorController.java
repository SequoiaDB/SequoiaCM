package com.sequoiacm.infrastructure.monitor.controller;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;
import com.sequoiacm.infrastructure.monitor.service.IMonitorCollectorService;

@RestController
@RequestMapping("/internal/v1/monitor_collector")
public class MonitorCollectorController {

    private final IMonitorCollectorService mcService;

    private static final Logger logger = LoggerFactory.getLogger(MonitorCollectorController.class);

    @Autowired
    public MonitorCollectorController(IMonitorCollectorService service) {
        this.mcService = service;
    }

    @GetMapping("/host_info")
    public Object getHostInfo() throws Exception {
        return mcService.getHostInfo();
    }

    @GetMapping("/gauge_response")
    public Object gauggeResponse() {
        return mcService.gaugeResponse();
    }

    @GetMapping("/show_flow")
    public Collection<WorkspaceFlow> showFlow() {
        return mcService.shwoFlow();
    }
}
