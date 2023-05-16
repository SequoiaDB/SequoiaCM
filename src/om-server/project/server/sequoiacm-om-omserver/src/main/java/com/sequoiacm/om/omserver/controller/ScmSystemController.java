package com.sequoiacm.om.omserver.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ScmSystemService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1/system")
public class ScmSystemController {

    @Autowired
    private ScmSystemService systemService;

    @PutMapping("/globalConfig")
    public void setGlobalConfig(ScmOmSession session,
            @RequestParam(value = RestParamDefine.CONFIG_NAME) String configName,
            @RequestParam(value = RestParamDefine.CONFIG_VALUE) String configValue)
            throws ScmInternalException, ScmOmServerException {
        systemService.setGlobalConfig(session, configName, configValue);
    }

    @GetMapping("/globalConfig")
    public Map<String, String> getGlobalConfig(ScmOmSession session,
            @RequestParam(value = RestParamDefine.CONFIG_NAME, required = false) String configName)
            throws ScmInternalException, ScmOmServerException {
        return systemService.getGlobalConfig(session, configName);
    }
}
