package com.sequoiacm.cloud.adminserver.controller;

import java.util.Collection;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.adminserver.service.IMonitorService;
import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    private static final Logger logger = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private IMonitorService ms;

    @GetMapping("/health")
    public BSONObject listHealth(@RequestParam(value = "name", required = false) String serviceName)
            throws Exception {
        return ms.listHealth(serviceName);
    }

    @GetMapping("/host_info")
    public BSONObject listHostInfo() throws Exception {
        return ms.listHostInfo();
    }

    @GetMapping("/show_flow")
    public Collection<WorkspaceFlow> showFlow() throws Exception {
        return ms.showFlow();
    }

    @GetMapping("/gauge_response")
    public BSONObject gaugeResponse() throws Exception {
        return ms.gaugeResponse();
    }
}
