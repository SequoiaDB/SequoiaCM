package com.sequoiacm.om.omserver.controller;

import java.util.List;

import org.bson.BSONObject;
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
import com.sequoiacm.om.omserver.core.ScmOmPasswordMgr;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.service.ScmUserService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmUserController {

    @Autowired
    private ScmUserService userservice;

    @Autowired
    private ScmOmPasswordMgr passwordMgr;

    @GetMapping("/users/{user_name:.+}")
    public OmUserInfo getUserInfo(ScmOmSession session, @PathVariable("user_name") String username)
            throws ScmInternalException, ScmOmServerException {
        return userservice.getUserInfo(session, username);
    }

    @PostMapping("/users/{user_name:.+}")
    public void createUser(ScmOmSession session, @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.USER_TYPE, required = false, defaultValue = "LOCAL") String userType,
            @RequestParam(value = RestParamDefine.PASSWORD, required = false) String password)
            throws ScmInternalException, ScmOmServerException {
        userservice.createUser(session, username, userType, passwordMgr.decrypt(password));
    }

    @DeleteMapping("/users/{user_name:.+}")
    public void deleteUser(ScmOmSession session, @PathVariable("user_name") String username)
            throws ScmInternalException, ScmOmServerException {
        userservice.deleteUser(session, username);
    }

    @PutMapping(value = "/users/{user_name:.+}", params = "action=change_password")
    public void changePassword(ScmOmSession session, @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.OLD_PASSWORD, required = false) String oldPassword,
            @RequestParam(value = RestParamDefine.NEW_PASSWORD, required = true) String newPassword,
            @RequestParam(value = RestParamDefine.CLEAB_SESSIONS, required = false, defaultValue = "false") boolean cleanSessions)
            throws ScmInternalException, ScmOmServerException {
        userservice.changePassword(session, username, passwordMgr.decrypt(oldPassword),
                passwordMgr.decrypt(newPassword), cleanSessions);
    }

    @PutMapping(value = "/users/{user_name:.+}", params = "action=grant_role")
    public void grantRoles(ScmOmSession session, @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.ROLES, required = true) List<String> roles)
            throws ScmInternalException, ScmOmServerException {
        userservice.grantRoles(session, username, roles);
    }

    @PutMapping(value = "/users/{user_name:.+}", params = "action=revoke_role")
    public void revokeRoles(ScmOmSession session, @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.ROLES, required = true) List<String> roles)
            throws ScmInternalException, ScmOmServerException {
        userservice.revokeRoles(session, username, roles);
    }

    @PutMapping(value = "/users/{user_name:.+}", params = "action=change_user_type")
    public void changeUserType(ScmOmSession session, @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.USER_TYPE, required = true) String userType,
            @RequestParam(value = RestParamDefine.NEW_PASSWORD, required = false) String newPassword,
            @RequestParam(value = RestParamDefine.OLD_PASSWORD, required = false) String oldPassword)
            throws ScmInternalException, ScmOmServerException {
        userservice.changeUserType(session, username, userType, passwordMgr.decrypt(oldPassword),
                passwordMgr.decrypt(newPassword));
    }

    @PutMapping(value = "/users/{user_name:.+}", params = "action=disable")
    public void disableUser(ScmOmSession session, @PathVariable("user_name") String username)
            throws ScmInternalException, ScmOmServerException {
        userservice.disableUser(session, username);
    }

    @PutMapping(value = "/users/{user_name:.+}", params = "action=enable")
    public void enableUser(ScmOmSession session, @PathVariable("user_name") String username)
            throws ScmInternalException, ScmOmServerException {
        userservice.enableUser(session, username);
    }

    @GetMapping("/users")
    public List<OmUserInfo> listUsers(ScmOmSession session,
            @RequestParam(value = RestParamDefine.CONDITION, required = false) BSONObject condition,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmInternalException, ScmOmServerException {
        return userservice.listUsers(session, condition, skip, limit);
    }
}
