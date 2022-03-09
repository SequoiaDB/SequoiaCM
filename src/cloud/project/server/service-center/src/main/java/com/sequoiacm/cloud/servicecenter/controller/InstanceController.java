package com.sequoiacm.cloud.servicecenter.controller;

import com.sequoiacm.cloud.servicecenter.common.RestDefine;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterError;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.service.InstanceService;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1")
public class InstanceController {

    @Autowired
    private InstanceService instanceService;

    @DeleteMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public void deleteInstance(@RequestParam(RestDefine.IP_ADDR) String ipAddr,
            @RequestParam(RestDefine.PORT) Integer port, Authentication auth)
            throws ScmServiceCenterException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();
        if (!scmUser.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            throw new ScmServiceCenterException(ScmServiceCenterError.UNAUTHORIZED_OPERATION,
                    "unauthorized operation, user no " + ScmRole.AUTH_ADMIN_ROLE_NAME + " role.");
        }
        instanceService.deleteInstance(ipAddr, port, scmUser.getUsername(),
                scmUser.getPasswordType().toString());
    }

}
