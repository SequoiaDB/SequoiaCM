package com.sequoiacm.cloud.authentication.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.authentication.service.IPrivilegeService;
import com.sequoiacm.infrastructrue.security.core.ScmPrivMeta;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;

@RequestMapping("/api/v1")
@RestController
public class PrivilegeController {
    private static final Logger logger = LoggerFactory.getLogger(PrivilegeController.class);

    @Autowired
    private IPrivilegeService privilegeService;

    @GetMapping(value = "/privileges")
    public ScmPrivMeta getPrivMeta() {
        logger.debug("received get meta request");
        return privilegeService.getMeta();
    }

    @GetMapping(value = "/relations")
    public List<ScmPrivilege> listPrivileges(
            @RequestParam(value = "role_id", required = false) String roleId,
            @RequestParam(value = "role_name", required = false) String roleName,
            @RequestParam(value = "resource_type", required = false) String resourceType,
            @RequestParam(value = "resource", required = false) String resource) {

        if (null != roleId) {
            return privilegeService.getPrivilegeListByRoleId(roleId);
        }

        if (null != roleName) {
            return privilegeService.getPrivilegeListByRoleName(roleName);
        }

        if (null != resourceType && null != resource) {
            return privilegeService.getPrivilegeListByResource(resourceType, resource);
        }

        return privilegeService.getPrivilegeList();
    }

    @GetMapping(value = "/relations/{privilege_id}")
    public ScmPrivilege getPrivilege(@PathVariable(value = "privilege_id") String privilegeId) {
        return privilegeService.getPrivilegeById(privilegeId);
    }

    @GetMapping(value = "/resources")
    public List<ScmResource> listResources(
            @RequestParam(value = "workspace_name", required = false) String workspaceName) {
        if (null != workspaceName) {
            return privilegeService.getResourceListByWorkspace(workspaceName);
        }

        return privilegeService.getResourceList();
    }

    @GetMapping(value = "/resources/{resource_id}")
    public ScmResource getResource(@PathVariable("resource_id") String resourceId) {
        return privilegeService.getResourceById(resourceId);
    }
}
