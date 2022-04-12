package com.sequoiacm.cloud.authentication.controller;

import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.cloud.authentication.exception.ForbiddenException;
import com.sequoiacm.cloud.authentication.exception.NotFoundException;
import com.sequoiacm.cloud.authentication.service.IPrivilegeService;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1")
@RestController
public class RoleController {
    @Autowired
    private ScmUserRoleRepository repository;

    @Autowired
    private IPrivilegeService privService;

    @Autowired
    private ScmAudit audit;

    @PostMapping("/roles/{roleName:.+}")
    public ScmRole createRole(@PathVariable("roleName") String roleName,
            @RequestParam(value = "description", required = false) String description,
            Authentication auth) {
        String innerRoleName = roleName;
        if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
            innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
        }

        if (repository.findRoleByName(innerRoleName) != null) {
            throw new BadRequestException("Role already exists: " + roleName);
        }
        ScmRole role = ScmRole.withRoleName(innerRoleName).roleId(repository.generateRoleId())
                .description(description).build();
        repository.insertRole(role);
        audit.info(ScmAuditType.CREATE_ROLE, auth, null, 0,
                "create new role, roleName=" + roleName);
        return role;
    }

    @DeleteMapping("/roles/{roleName:.+}")
    public void deleteRole(@PathVariable("roleName") String roleName, Authentication auth)
            throws Exception {
        String innerRoleName = roleName;
        if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
            innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
        }

        if (ScmRole.AUTH_ADMIN_ROLE_NAME.equals(innerRoleName) ||
                ScmRole.AUTH_MONITOR_ROLE_NAME.equals(innerRoleName)) {
            throw new ForbiddenException("Cannot delete role AUTH_ADMIN");
        }
        ScmRole role = repository.findRoleByName(innerRoleName);
        if (role == null) {
            throw new NotFoundException("Role is not found: " + roleName);
        }

        privService.deleteRole(role);

        audit.info(ScmAuditType.DELETE_ROLE, auth, null, 0, "delete role, roleName=" + roleName);
    }

    @GetMapping("/roles")
    public List<ScmRole> findAllRoles(
            @RequestParam(value = "order_by", required = false) BSONObject orderBy,
            @RequestParam(value = "skip", required = false, defaultValue = "0") long skip,
            @RequestParam(value = "limit", required = false, defaultValue = "-1") long limit,
            Authentication auth) {
        if (skip < 0) {
            throw new BadRequestException("skip can not be less than 0");
        }
        if (limit < -1) {
            throw new BadRequestException("limit can not be less than -1");
        }
        audit.info(ScmAuditType.USER_DQL, auth, null, 0, "find all roles");
        return repository.findAllRoles(orderBy, skip, limit);
    }

    @GetMapping("/roles/{roleName:.+}")
    public ScmRole findRole(@PathVariable("roleName") String roleName, Authentication auth) {
        String innerRoleName = roleName;
        if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
            innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
        }

        ScmRole role = repository.findRoleByName(innerRoleName);
        if (role == null) {
            throw new NotFoundException("Role is not found: " + roleName);
        }
        audit.info(ScmAuditType.USER_DQL, auth, null, 0, "find role by roleName=" + roleName);
        return role;
    }

    @GetMapping("/roles/id/{roleId}")
    public ScmRole findRoleById(@PathVariable("roleId") String roleId, Authentication auth) {
        ScmRole role = repository.findRoleById(roleId);
        if (role == null) {
            throw new NotFoundException("Role id not found by id: " + roleId);
        }
        audit.info(ScmAuditType.USER_DQL, auth, null, 0, "find role by roleId=" + roleId);
        return role;
    }
}
