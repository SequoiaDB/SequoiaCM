package com.sequoiacm.om.omserver.remote;

import com.sequoiacm.infrastructure.common.SecurityRestField;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.monitor.OmMonitorInstanceBasicInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface ScmServiceCenterFeignClient {

    @GetMapping("/internal/v1/instances")
    List<OmMonitorInstanceBasicInfo> getInstanceList() throws ScmFeignException;

    @DeleteMapping(value = "/api/v1/instances")
    void deleteInstance(@RequestParam(RestParamDefine.IP_ADDR) String ipAddr,
            @RequestParam(RestParamDefine.PORT) Integer port,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId)
            throws ScmFeignException;
}
