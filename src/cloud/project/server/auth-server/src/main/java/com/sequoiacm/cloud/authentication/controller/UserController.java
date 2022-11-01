package com.sequoiacm.cloud.authentication.controller;

import java.util.List;
import com.sequoiacm.cloud.authentication.service.IUserService;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.sequoiacm.cloud.authentication.dao.IPrivVersionDao;
import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.cloud.authentication.exception.ForbiddenException;
import com.sequoiacm.cloud.authentication.exception.NotFoundException;
import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;

@RequestMapping("/api")
@RestController
public class UserController {
    @Autowired
    private ScmUserRoleRepository userRoleRepository;

    @Autowired
    private SequoiadbSessionRepository sessionRepository;

    @Autowired
    private IPrivVersionDao versionDao;

    @Autowired
    private ITransaction transactionFactory;

    @Autowired
    private ScmAudit audit;

    @Autowired
    private IUserService userService;

    @PostMapping("/v1/users/{username:.+}")
    public ScmUser createUser(@PathVariable("username") String username,
            @RequestParam(value = "password_type", required = false) String type,
            @RequestParam(value = "password", required = false) String password,
            Authentication auth) throws Exception {
        return userService.createUser(username, type, password, auth, false);
    }

    @PostMapping("/v2/users/{username:.+}")
    public ScmUser v2CreateUser(@PathVariable("username") String username,
            @RequestParam(value = "password_type", required = false) String type,
            @RequestParam(value = "password", required = false) String password,
            Authentication auth) throws Exception {
        return userService.createUser(username, type, password, auth, true);
    }

    @DeleteMapping("/v1/users/{username:.+}")
    public void deleteUser(@PathVariable("username") String username, Authentication authentication)
            throws Exception {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Invalid username");
        }

        ScmUser currentUser = (ScmUser) authentication.getPrincipal();
        if (username.equals(currentUser.getUsername())) {
            throw new ForbiddenException("Cannot delete current user");
        }

        ScmUser user = userRoleRepository.findUserByName(username);
        if (user == null) {
            throw new NotFoundException("User is not found: " + username);
        }

        if (user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            List<ScmUser> users = userRoleRepository
                    .findUsersByRoleName(ScmRole.AUTH_ADMIN_ROLE_NAME);
            if (users.size() <= 1) {
                throw new ForbiddenException("Cannot delete the last AUTH_ADMIN user");
            }
        }

        deleteUser(user);

        audit.info(ScmAuditType.DELETE_USER, authentication, null, 0, "delete user: userName=" + username);
    }

    private void deleteUser(ScmUser user) throws Exception {
        ITransaction t = transactionFactory.createTransation();
        try {
            t.begin();
            userRoleRepository.deleteUser(user, t);
            sessionRepository.deleteSessions(user.getUsername());
            versionDao.incVersion(t);

            t.commit();
        }
        catch (Exception e) {
            t.rollback();
            throw e;
        }
    }

    @PutMapping("/v1/users/{username:.+}")
    public ScmUser alterUser(Authentication authentication,
            @PathVariable("username") String username,
            @RequestParam(value = "old_password", required = false) String oldPassword,
            @RequestParam(value = "new_password", required = false) String newPassword,
            @RequestParam(value = "password_type", required = false) ScmUserPasswordType passwordType,
            @RequestParam(value = "add_roles", required = false) List<String> addRoles,
            @RequestParam(value = "del_roles", required = false) List<String> delRoles,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "clean_sessions", required = false) Boolean cleanSessions)
            throws Exception {
        return userService.alterUser(authentication, username, oldPassword, newPassword,
                passwordType, addRoles, delRoles, enabled, cleanSessions, false);
    }

    @PutMapping("/v2/users/{username:.+}")
    public ScmUser v2AlterUser(Authentication authentication,
            @PathVariable("username") String username,
            @RequestParam(value = "old_password", required = false) String oldPassword,
            @RequestParam(value = "new_password", required = false) String newPassword,
            @RequestParam(value = "password_type", required = false) ScmUserPasswordType passwordType,
            @RequestParam(value = "add_roles", required = false) List<String> addRoles,
            @RequestParam(value = "del_roles", required = false) List<String> delRoles,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "clean_sessions", required = false) Boolean cleanSessions)
            throws Exception {
        return userService.alterUser(authentication, username, oldPassword, newPassword,
                passwordType, addRoles, delRoles, enabled, cleanSessions, true);
    }

    @GetMapping("/v1/users")
    public List<ScmUser> findAllUsers(
            @RequestParam(value = "password_type", required = false) ScmUserPasswordType type,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "has_role", required = false) String roleName,
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
        String innerRoleName = null;
        if (StringUtils.hasText(roleName)) {
            if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
                innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
            }
            else {
                innerRoleName = roleName;
            }
        }

        List<ScmUser> findAllUsers = userRoleRepository.findAllUsers(type, enabled, innerRoleName,
                orderBy, skip, limit);
        audit.info(ScmAuditType.USER_DQL, auth, null, 0, "find all users");
        return findAllUsers;
    }

    @GetMapping("/v1/users/{username:.+}")
    public ScmUser findUser(@PathVariable("username") String username, Authentication auth) {
        ScmUser user = userRoleRepository.findUserByName(username);
        if (user == null) {
            throw new NotFoundException("User is not found: " + username);
        }

        audit.info(ScmAuditType.USER_DQL, auth, null, 0, "find user by userName=" + username);
        return user;
    }

    @GetMapping("/v2/salt/{username:.+}")
    public BSONObject getSalt(@PathVariable("username") String username) throws Exception {
        return userService.findUserSalt(username);
    }
}
