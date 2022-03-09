package com.sequoiacm.cloud.servicecenter.controller;

import com.sequoiacm.cloud.servicecenter.common.RestDefine;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.model.ScmInstance;
import com.sequoiacm.cloud.servicecenter.service.InstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/v1")
public class InternalInstanceController {

    @Autowired
    private InstanceService instanceService;

    @GetMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ScmInstance> listInstances() throws ScmServiceCenterException {
        return instanceService.listInstances();
    }

    @PutMapping(value = "/instances", params = "action=stop", produces = MediaType.APPLICATION_JSON_VALUE)
    public void stopInstance(@RequestParam(RestDefine.IP_ADDR) String ipAddr,
            @RequestParam(RestDefine.PORT) Integer port) throws ScmServiceCenterException {
        instanceService.stopInstance(ipAddr, port);
    }
}
