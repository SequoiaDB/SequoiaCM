package com.sequoiacm.contentserver.controller;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.service.IPrivilegeService;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.common.SecurityRestField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PrivilegeController {

    private final IPrivilegeService privilegeService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    public PrivilegeController(IPrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    @PutMapping("roles/{role_name}/grant")
    public void grantPrivilege(@RequestHeader(value = SecurityRestField.SESSION_ATTRIBUTE) String token,
            @PathVariable("role_name") String roleName,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("resource") String resource, @RequestParam("privilege") String privilege,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (null == user || null == roleName || null == resourceType || null == resource
                || null == privilege) {
            throw new ScmInvalidArgumentException(
                    "parameter is null:user=" + user + ",roleName=" + roleName + ",resourceType="
                            + resourceType + ",resource=" + resource + ",privilege=" + privilege);
        }

        // check priority in privilegeService.grant()
        privilegeService.grant(token, user, roleName, resourceType, resource, privilege);

    }

    @PutMapping("roles/{role_name}/revoke")
    public void revokePrivilege(@RequestHeader(value = SecurityRestField.SESSION_ATTRIBUTE) String token,
            @PathVariable("role_name") String roleName,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("resource") String resource, @RequestParam("privilege") String privilege,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (null == user || null == roleName || null == resourceType || null == resource
                || null == privilege) {
            throw new ScmInvalidArgumentException(
                    "parmeter is null:user=" + user + ",roleName=" + roleName + ",resourceType="
                            + resourceType + ",resource=" + resource + ",privilege=" + privilege);
        }

        // check priority in privilegeService.revoke()
        privilegeService.revoke(token, user, roleName, resourceType, resource, privilege);
    }
}
