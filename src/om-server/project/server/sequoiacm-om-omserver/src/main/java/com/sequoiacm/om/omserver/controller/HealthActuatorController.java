package com.sequoiacm.om.omserver.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthActuatorController {

    @GetMapping(value = "/internal/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> health() {
        HashMap<String, String> m = new HashMap<>();
        m.put("status", "UP");
        return m;
    }
}
