package com.sequoiacm.cloud.authentication.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.cloud.authentication.exception.NotFoundException;
import com.sequoiacm.cloud.authentication.service.IPrivilegeService;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;

@RequestMapping("/internal/v1")
@RestController
public class InternalRoleController {

    @Autowired
    private ScmUserRoleRepository repository;

    @Autowired
    private IPrivilegeService privService;

    @Autowired
    private ScmAudit audit;

    @PutMapping("/roles/{role_name:.+}/grant")
    public void grant(@PathVariable("role_name") String roleName,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("resource") String resource, @RequestParam("privilege") String privilege,
            Authentication auth) throws Exception {
        if (null == roleName || null == resourceType || null == resource || null == privilege) {
            throw new BadRequestException("role_name=" + roleName + ",resource_type="
                    + resourceType + ",resource=" + resource + ",privilege=" + privilege);
        }

        String innerRoleName = roleName;
        if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
            innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
        }

        ScmRole role = repository.findRoleByName(innerRoleName);
        if (role == null) {
            throw new NotFoundException("Role is not found: " + roleName);
        }

        privService.grantPrivilege(ScmPrivilege.JSON_VALUE_ROLE_TYPE_ROLE, role.getRoleId(),
                resourceType, resource, privilege);
        audit.info(ScmAuditType.GRANT, auth, null, 0, "grant role, roleName=" + roleName
                + ",resource_type=" + resourceType + ",resource=" + resource + ",privilege=" + privilege);
    }

    @PutMapping("/roles/{role_name:.+}/revoke")
    public void revoke(@PathVariable("role_name") String roleName,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("resource") String resource, @RequestParam("privilege") String privilege,
            Authentication auth) throws Exception {
        if (null == roleName || null == resourceType || null == resource || null == privilege) {
            throw new BadRequestException("role_name=" + roleName + ",resource_type="
                    + resourceType + ",resource=" + resource + ",privilege=" + privilege);
        }

        String innerRoleName = roleName;
        if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
            innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
        }

        ScmRole role = repository.findRoleByName(innerRoleName);
        if (role == null) {
            throw new NotFoundException("Role is not found: " + roleName);
        }

        privService.revokePrivilege(ScmPrivilege.JSON_VALUE_ROLE_TYPE_ROLE, role.getRoleId(),
                resourceType, resource, privilege);

        audit.info(ScmAuditType.REVOKE, auth, null, 0, "grant role, roleName=" + roleName
                + ",resource_type=" + resourceType + ",resource=" + resource + ",privilege=" + privilege);
    }
}