package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ServiceCenterService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class ServiceCenterController {

    @Autowired
    private ServiceCenterService service;

    @GetMapping("/services/zones")
    public Set<String> listZones(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SERVICE_REGION, required = true) String region)
            throws ScmOmServerException, ScmInternalException {
        return service.listZones(session, region);
    }

    @GetMapping("/services/regions")
    public Set<String> listRegions(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        return service.listRegions(session);
    }
}
