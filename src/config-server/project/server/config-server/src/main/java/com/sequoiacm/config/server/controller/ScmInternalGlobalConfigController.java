package com.sequoiacm.config.server.controller;

import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.server.service.ScmGlobalConfigServiceImpl;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1")
public class ScmInternalGlobalConfigController {

    @Autowired
    private ScmGlobalConfigServiceImpl service;

    @GetMapping("/globalConfig")
    public Map<String, String> getGlobalConfig(
            @RequestParam(value = ScmRestArgDefine.GLOBAL_CONF_NAME, required = false) String configName)
            throws MetasourceException {
        return service.getGlobalConfig(configName);
    }

}
