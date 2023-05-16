package com.sequoiacm.config.server.controller;

import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.module.ScmGlobalConfig;
import com.sequoiacm.config.server.service.ScmGlobalConfigServiceImpl;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ScmGlobalConfigController {
    @Autowired
    private ScmGlobalConfigServiceImpl service;

    @PutMapping("/globalConfig")
    public void setGlobalConfig(
            @RequestParam(value = ScmRestArgDefine.GLOBAL_CONF_NAME) String configName,
            @RequestParam(value = ScmRestArgDefine.GLOBAL_CONF_VALUE) String configValue)
            throws MetasourceException {
        service.setGlobalConfig(configName, configValue);
    }

    @GetMapping("/globalConfig")
    public Map<String, String> getGlobalConfig(
            @RequestParam(value = ScmRestArgDefine.GLOBAL_CONF_NAME, required = false) String configName)
            throws MetasourceException {
        return service.getGlobalConfig(configName);
    }

}
