package com.sequoiacm.om.omserver.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.service.ScmRoleService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmRoleController {

    @Autowired
    private ScmRoleService roleService;

    @GetMapping("/roles/{role_name}")
    public OmRoleInfo getRole(ScmOmSession session,
            @PathVariable("role_name") String rolename)
            throws ScmInternalException, ScmOmServerException {
        return roleService.getRole(session, rolename);
    }

    @PostMapping("/roles/{role_name}")
    public void createRole(ScmOmSession session,
            @PathVariable("role_name") String rolename,
            @RequestParam(value = RestParamDefine.DESCRIPTION, required = false) String description)
            throws ScmInternalException, ScmOmServerException {
        roleService.createRole(session, rolename, description);
    }

    @DeleteMapping("/roles/{role_name}")
    public void deleteRole(ScmOmSession session,
            @PathVariable("role_name") String rolename)
            throws ScmInternalException, ScmOmServerException {
        roleService.deleteRole(session, rolename);
    }

    @PutMapping(value = "/roles/{role_name}", params = "action=grant")
    public void grantPrivilege(ScmOmSession session,
            @PathVariable("role_name") String rolename,
            @RequestParam(value = RestParamDefine.RESOURCE_TYPE, required = true) String resourceType,
            @RequestParam(value = RestParamDefine.RESOURCE, required = true) String resource,
            @RequestParam(value = RestParamDefine.PRIVILEGE, required = true) String privilege)
            throws ScmOmServerException, ScmInternalException {
        roleService.grantPrivilege(session, rolename, resourceType, resource, privilege);
    }

    @PutMapping(value = "/roles/{role_name}", params = "action=revoke")
    public void revokePrivilege(ScmOmSession session,
            @PathVariable("role_name") String rolename,
            @RequestParam(value = RestParamDefine.RESOURCE_TYPE, required = true) String resourceType,
            @RequestParam(value = RestParamDefine.RESOURCE, required = true) String resource,
            @RequestParam(value = RestParamDefine.PRIVILEGE, required = true) String privilege)
            throws ScmOmServerException, ScmInternalException {
        roleService.revokePrivilege(session, rolename, resourceType, resource, privilege);
    }

    @GetMapping("/roles")
    public List<OmRoleBasicInfo> listRoles(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmInternalException, ScmOmServerException {
        return roleService.listRoles(session, skip, limit);
    }
}
