package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;

public interface ContentServerClientInternalMetrics extends ContentServerClient {

    // ***************internal metrics***************//
    @GetMapping(value = "/internal/v1/metrics")
    public Map<String, Object> metrics() throws Exception;

}
