package com.sequoiacm.om.omserver.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.core.ScmOmPasswordMgr;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ScmDockingService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
public class ScmDockingController {

    @Autowired
    private ScmDockingService dockingService;

    @Autowired
    private ScmOmPasswordMgr passwordMgr;

    @PostMapping("/dock")
    public ResponseEntity<Object> dock(@RequestParam(RestParamDefine.GATEWAY_ADDR) List<String> gatewayList,
            @RequestParam(RestParamDefine.REGION) String region,
            @RequestParam(RestParamDefine.ZONE) String zone,
            @RequestParam(RestParamDefine.USERNAME) String username,
            @RequestParam(RestParamDefine.PASSWORD) String encryptedPassword)
            throws ScmInternalException, ScmOmServerException {
        String decrypedPwd = passwordMgr.decrypt(encryptedPassword);
        ScmOmSession s = dockingService.dock(gatewayList, region, zone, username, decrypedPwd);
        return ResponseEntity.ok().header(RestParamDefine.X_AUTH_TOKEN, s.getSessionId()).build();
    }
}
