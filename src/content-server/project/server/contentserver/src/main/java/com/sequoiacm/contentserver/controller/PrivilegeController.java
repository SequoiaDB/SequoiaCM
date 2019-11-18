package com.sequoiacm.contentserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.service.IPrivilegeService;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.security.auth.RestField;

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
    public void grantPrivilege(@RequestHeader(value = RestField.SESSION_ATTRIBUTE) String token,
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
        audit.info(ScmAuditType.GRANT, auth, null, 0, "grant privilege, user=" + user + ","
                + "roleName=" + roleName + ",resourceType="+ resourceType + ",resource=" + resource + ",privilege=" + privilege);
    }

    @PutMapping("roles/{role_name}/revoke")
    public void revokePrivilege(@RequestHeader(value = RestField.SESSION_ATTRIBUTE) String token,
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
        audit.info(ScmAuditType.REVOKE, auth, null, 0, "grant privilege, user=" + user + ","
                + "roleName=" + roleName + ",resourceType="+ resourceType + ",resource=" + resource + ",privilege=" + privilege);
    }
}
