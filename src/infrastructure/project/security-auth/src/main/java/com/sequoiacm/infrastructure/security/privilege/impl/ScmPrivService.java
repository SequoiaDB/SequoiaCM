package com.sequoiacm.infrastructure.security.privilege.impl;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructrue.security.core.ScmPrivMeta;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructure.security.auth.RestField;


// @FeignClient(name = "auth-server", configuration =
// ScmPrivServiceConfig.class)
public interface ScmPrivService {
    @GetMapping("/api/v1/privileges")
    public ScmPrivMeta getPrivMeta();

    @GetMapping("/api/v1/relations")
    public List<ScmPrivilege> listPrivileges(
            @RequestParam(value = "role_id", required = false) String roleId,
            @RequestParam(value = "role_name", required = false) String roleName,
            @RequestParam(value = "resource_type", required = false) String resourceType,
            @RequestParam(value = "resource", required = false) String resource);

    @GetMapping("/api/v1/resources")
    public List<ScmResource> listResources(
            @RequestParam(value = "workspace_name", required = false) String wsName);

    @GetMapping("/api/v1/users")
    public List<ScmUser> findAllUsers(
            @RequestParam(value = "password_type", required = false) ScmUserPasswordType type,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "has_role", required = false) String roleName);

    @GetMapping("/api/v1/roles")
    public List<ScmRole> findAllRoles();

    @GetMapping("/api/v1/roles/id/{roleId}")
    public ScmRole findRoleById(@PathVariable("roleId") String roleId);

    @PutMapping("/internal/v1/roles/{role_name}/grant")
    public void grant(@RequestHeader(value = RestField.SESSION_ATTRIBUTE) String token,
            @PathVariable("role_name") String roleName,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("resource") String resource, @RequestParam("privilege") String privilege);

    @PutMapping("/internal/v1/roles/{role_name}/revoke")
    public void revoke(@RequestHeader(value = RestField.SESSION_ATTRIBUTE) String token,
            @PathVariable("role_name") String roleName,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("resource") String resource, @RequestParam("privilege") String privilege);
}
