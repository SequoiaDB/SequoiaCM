package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.service.OmMonitorService;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.monitor.*;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor")
public class OmMonitorController {

    @Autowired
    private OmMonitorService omMonitorService;

    @GetMapping("/instances")
    public List<OmMonitorInstanceInfo> listInstances() throws ScmOmServerException {
        return omMonitorService.listInstances();
    }

    @GetMapping("/version")
    public Map<String, Object> versionInfo() {
        return omMonitorService.getVersionInfo();
    }

    @GetMapping(value = "/instances/{instance_id:.+}")
    public OmMonitorInstanceInfo getInstanceInfo(@PathVariable("instance_id") String instanceId)
            throws ScmOmServerException {
        return omMonitorService.checkAndGetInstance(instanceId);
    }

    @DeleteMapping(value = "/instances/{instance_id:.+}")
    public void deleteInstance(ScmOmSession session, @PathVariable("instance_id") String instanceId)
            throws ScmOmServerException, ScmInternalException {
        omMonitorService.deleteInstance(session, instanceId);
    }

    @GetMapping(value = "/instances/{instance_id:.+}", params = "action=getConnectionInfo")
    public OmConnectionInfo getConnectionInfo(@PathVariable("instance_id") String instanceId,
            ScmOmSession session)
            throws ScmOmServerException {
        return omMonitorService.getConnectionInfo(instanceId, session);
    }

    @GetMapping(value = "/instances/{instance_id:.+}", params = "action=getHeapInfo")
    public OmHeapInfo getHeapInfo(@PathVariable("instance_id") String instanceId,
            ScmOmSession session)
            throws ScmOmServerException {
        return omMonitorService.getHeapInfo(instanceId, session);
    }

    @GetMapping(value = "/instances/{instance_id:.+}", params = "action=getThreadInfo")
    public OmThreadInfo getThreadInfo(@PathVariable("instance_id") String instanceId,
            ScmOmSession session)
            throws ScmOmServerException {
        return omMonitorService.getThreadInfo(instanceId, session);
    }

    @GetMapping(value = "/instances/{instance_id:.+}", params = "action=getProcessInfo")
    public OmProcessInfo getProcessInfo(@PathVariable("instance_id") String instanceId,
            ScmOmSession session)
            throws ScmOmServerException {
        return omMonitorService.getProcessInfo(instanceId, session);
    }

    @GetMapping(value = "/instances/{instance_id:.+}", params = "action=getConfigInfo")
    public Map<String, String> getConfigInfo(@PathVariable("instance_id") String instanceId,
            ScmOmSession session)
            throws ScmOmServerException {
        return omMonitorService.getConfigInfo(instanceId, session);
    }

}
