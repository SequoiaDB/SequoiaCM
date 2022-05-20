package com.sequoiacm.om.omserver.remote;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.om.omserver.module.monitor.OmConnectionInfo;
import com.sequoiacm.om.omserver.module.monitor.OmProcessInfo;
import com.sequoiacm.om.omserver.module.monitor.OmThreadInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

public interface OmMonitorFeignClient {

    @GetMapping("/connection_info")
    OmConnectionInfo getConnectionInfo() throws ScmFeignException;

    @GetMapping("/connection_info")
    OmConnectionInfo getConnectionInfoWithAuth(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId) throws ScmFeignException;

    @GetMapping("/thread_info")
    OmThreadInfo getThreadInfo() throws ScmFeignException;

    @GetMapping("/thread_info")
    OmThreadInfo getThreadInfoWithAuth(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId)
            throws ScmFeignException;

    @GetMapping("/metrics/heap.*")
    Map<String, Object> getHeapInfo() throws ScmFeignException;

    @GetMapping("/metrics/heap.*")
    Map<String, Object> getHeapInfoWithAuth(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId) throws ScmFeignException;

    @GetMapping("/process_info")
    OmProcessInfo getProcessInfo() throws ScmFeignException;

    @GetMapping("/process_info")
    OmProcessInfo getProcessInfoWithAuth(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId) throws ScmFeignException;

    @GetMapping("/env")
    Map<?, ?> getEnvironmentInfo() throws ScmFeignException;

    @GetMapping("/env")
    Map<?, ?> getEnvironmentInfoWithAuth(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId) throws ScmFeignException;

}
