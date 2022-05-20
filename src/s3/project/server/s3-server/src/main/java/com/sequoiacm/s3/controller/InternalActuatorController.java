package com.sequoiacm.s3.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalActuatorController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private MetricsEndpoint metricsEndpoint;

    @Autowired
    private EnvironmentEndpoint environmentEndpoint;

    @GetMapping(value = "/health", params = "action=actuator")
    public Object health() {
        return healthEndpoint.invoke();
    }

    @GetMapping(value = "/metrics", params = "action=actuator")
    public Object metrics() {
        return metricsEndpoint.invoke();
    }

    @GetMapping(value = "/env", params = "action=actuator")
    public Object env() {
        return environmentEndpoint.invoke();
    }

}
