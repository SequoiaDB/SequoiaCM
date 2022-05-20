package com.sequoiacm.infrastructure.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(prefix = "scm.internalActuatorController", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(Endpoint.class)
@RestController
@RequestMapping("/internal/v1")
public class InternalActuatorController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private MetricsEndpoint metricsEndpoint;

    @Autowired
    private EnvironmentEndpoint environmentEndpoint;

    @GetMapping("/health")
    public Object health() {
        return healthEndpoint.invoke();
    }

    @GetMapping("/metrics")
    public Object metrics() {
        return metricsEndpoint.invoke();
    }

    @GetMapping("/env")
    public Object env() {
        return environmentEndpoint.invoke();
    }

}
