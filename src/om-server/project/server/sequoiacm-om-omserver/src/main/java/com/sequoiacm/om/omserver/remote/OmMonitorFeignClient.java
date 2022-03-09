package com.sequoiacm.om.omserver.remote;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.om.omserver.module.monitor.OmConnectionInfo;
import com.sequoiacm.om.omserver.module.monitor.OmProcessInfo;
import com.sequoiacm.om.omserver.module.monitor.OmThreadInfo;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

public interface OmMonitorFeignClient {

    @GetMapping("/connection_info")
    OmConnectionInfo getConnectionInfo() throws ScmFeignException;

    @GetMapping("/thread_info")
    OmThreadInfo getThreadInfo() throws ScmFeignException;

    @GetMapping("/metrics/heap.*")
    Map<String, Object> getHeapInfo() throws ScmFeignException;

    @GetMapping("/process_info")
    OmProcessInfo getProcessInfo() throws ScmFeignException;

    @GetMapping("/env")
    Map<?, ?> getEnvironmentInfo() throws ScmFeignException;

}
