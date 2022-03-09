package com.sequoiacm.infrastructure.monitor.core;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@RequestMapping
public interface InstanceHealthInfoProvider {

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    Object getHealthInfo();
}
