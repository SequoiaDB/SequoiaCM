package com.sequoiacm.cloud.authentication.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
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

import com.sequoiacm.cloud.authentication.config.TokenConfig;
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

@RequestMapping("/api/v1")
@RestController
public class UserController {
    @Autowired
    private ScmUserRoleRepository userRoleRepository;

    @Autowired
    private SequoiadbSessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IPrivVersionDao versionDao;

    @Autowired
    private ITransaction transactionFactory;

    @Autowired
    private TokenConfig tokenConfig;

    @Autowired
    private ScmAudit audit;

    @Autowired
    private SaltSource saltSource;

    @PostMapping("/users/{username:.+}")
    public ScmUser createUser(@PathVariable("username") String username,
            @RequestParam(value = "password_type", required = false) String type,
            @RequestParam(value = "password", required = false) String password,
            Authentication auth) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Invalid username");
        }

        if (userRoleRepository.findUserByName(username) != null) {
            throw new BadRequestException("User already exists: " + username);
        }

        ScmUserPasswordType passwordType = ScmUserPasswordType.LOCAL;
        if (type != null) {
            passwordType = ScmUserPasswordType.valueOf(type);
        }

        String passwd = "";
        if (passwordType == ScmUserPasswordType.LOCAL) {
            if (!StringUtils.hasText(password)) {
                throw new BadRequestException("Invalid password");
            }
            passwd = passwordEncoder.encodePassword(password, null);
        }

        if (passwordType == ScmUserPasswordType.TOKEN && !tokenConfig.isEnabled()) {
            throw new BadRequestException("Token password type is disabled");
        }

        ScmUser user = ScmUser.withUsername(username).userId(userRoleRepository.generateUserId())
                .passwordType(passwordType).password(passwd).build();
        userRoleRepository.insertUser(user);
        audit.info(ScmAuditType.CREATE_USER, auth, null, 0, "create user: userName=" + username);

        return user;
    }

    @DeleteMapping("/users/{username:.+}")
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

    @PutMapping("/users/{username:.+}")
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
        if (newPassword != null && "".equals(newPassword.trim())) {
            throw new BadRequestException("Invalid new passwod, password is empty");
        }
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Invalid username");
        }
        ScmUser user = userRoleRepository.findUserByName(username);
        if (user == null) {
            throw new NotFoundException("User is not found: " + username);
        }

        if (user.getPasswordType() == ScmUserPasswordType.LDAP && StringUtils.hasText(newPassword)
                && (passwordType == null || passwordType == ScmUserPasswordType.LDAP)) {
            throw new BadRequestException("Cannot change password for LDAP user: " + username);
        }

        if (user.getPasswordType() == ScmUserPasswordType.TOKEN && StringUtils.hasText(newPassword)
                && (passwordType == null || passwordType == ScmUserPasswordType.TOKEN)) {
            throw new BadRequestException("Cannot change password for TOKEN user: " + username);
        }

        if (user.getPasswordType() == ScmUserPasswordType.LDAP && StringUtils.isEmpty(newPassword)
                && passwordType != null && passwordType == ScmUserPasswordType.LOCAL) {
            throw new BadRequestException(
                    "Cannot change LDAP user to LOCAL password type without password: " + username);
        }

        if (user.getPasswordType() == ScmUserPasswordType.TOKEN && StringUtils.isEmpty(newPassword)
                && passwordType != null && passwordType == ScmUserPasswordType.LOCAL) {
            throw new BadRequestException(
                    "Cannot change TOKEN user to LOCAL password type without password: "
                            + username);
        }

        boolean isCurrentUser = false;
        ScmUser currentUser = (ScmUser) authentication.getPrincipal();
        if (username.equals(currentUser.getUsername())) {
            isCurrentUser = true;
        }

        boolean altered = false;
        boolean securityAltered = false;
        ScmUser.ScmUserBuilder builder = ScmUser.withUsername(username).userId(user.getUserId());
        String message = "alter user : ";
        // process password
        if (StringUtils.hasText(newPassword) && !newPassword.equals(user.getPassword())) {
            boolean isAuthAdmin = false;
            for (ScmRole role : user.getAuthorities()) {
                if (role.isAuthAdmin()) {
                    isAuthAdmin = true;
                    break;
                }
            }

            if (isAuthAdmin) {
                if (!StringUtils.hasText(oldPassword)) {
                    throw new BadRequestException("Missing old password for user " + username);
                }

                Object salt = saltSource.getSalt(user);
                if (!passwordEncoder.isPasswordValid(user.getPassword(), oldPassword, salt)) {
                    throw new BadRequestException("Incorrect old password for user " + username);
                }
            }

            builder.password(passwordEncoder.encodePassword(newPassword, null));

            altered = true;
            securityAltered = true;
            message += "alter password;";
        }
        else {
            builder.password(user.getPassword());
        }

        // process passwordType
        if (passwordType != null && passwordType != user.getPasswordType()) {
            builder.passwordType(passwordType);
            altered = true;
            message += " alter passwordType;";
        }
        else {
            builder.passwordType(user.getPasswordType());
        }

        boolean isAlterRoles = false;
        // process enabled
        if (enabled != null && enabled != user.isEnabled()) {
            if (isCurrentUser) {
                throw new ForbiddenException("Cannot disable current user");
            }
            builder.disabled(!enabled);
            altered = true;
            isAlterRoles = true;
            securityAltered = true;
            message += " alter enabled=" + enabled + ";";
        }
        else {
            builder.disabled(!user.isEnabled());
        }

        // process addRoles and delRoles
        HashSet<ScmRole> originRoles = new HashSet<>(user.getAuthorities());
        HashSet<ScmRole> roles = new HashSet<>(user.getAuthorities());
        Map<String, ScmRole> roleNamesMap = new HashMap<>();
        for (ScmRole role : roles) {
            roleNamesMap.put(role.getRoleName(), role);
        }

        if (addRoles != null && addRoles.size() > 0) {
            for (String roleName : addRoles) {
                if (!StringUtils.hasText(roleName)) {
                    continue;
                }
                String innerRoleName = roleName;
                if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
                    innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
                }
                if (!roleNamesMap.containsKey(innerRoleName)) {
                    ScmRole role = userRoleRepository.findRoleByName(innerRoleName);
                    if (role == null) {
                        throw new BadRequestException("Invalid role name: " + roleName);
                    }
                    roles.add(role);
                    roleNamesMap.put(role.getRoleName(), role);
                    message += " alter addRoles,roleName=" + role.getRoleName() + ";";
                }
            }
        }

        if (delRoles != null && delRoles.size() > 0) {
            for (String roleName : delRoles) {
                if (!StringUtils.hasText(roleName)) {
                    continue;
                }
                String innerRoleName = roleName;
                if (!roleName.startsWith(ScmRole.ROLE_NAME_PREFIX)) {
                    innerRoleName = ScmRole.ROLE_NAME_PREFIX + roleName;
                }
                if (roleNamesMap.containsKey(innerRoleName)) {
                    ScmRole role = roleNamesMap.get(innerRoleName);
                    if (isCurrentUser & role.isAuthAdmin()) {
                        throw new ForbiddenException(
                                "Cannot delete current user's AUTH_ADMIN role");
                    }
                    roles.remove(role);
                    roleNamesMap.remove(innerRoleName);
                    message += " alter delRoles,roleName=" + role.getRoleName() + ";";
                }
            }
        }

        if (!originRoles.equals(roles)) {
            builder.roles(roles);
            altered = true;
            isAlterRoles = true;
        }
        else {
            builder.roles(originRoles);
        }

        if (altered) {
            ScmUser newUser = builder.build();
            updateUser(newUser, isAlterRoles);

            if (securityAltered && cleanSessions != null && cleanSessions && !isCurrentUser) {
                sessionRepository.deleteSessions(username);
            }

            audit.info(ScmAuditType.UPDATE_USER, authentication, null, 0, message);
            return newUser;
        }
        else {
            return user;
        }
    }

    private void updateUser(ScmUser newUser, boolean isAlterRoles) throws Exception {
        if (!isAlterRoles) {
            userRoleRepository.updateUser(newUser, null);
            return;
        }

        ITransaction t = transactionFactory.createTransation();
        try {
            t.begin();
            userRoleRepository.updateUser(newUser, t);
            versionDao.incVersion(t);

            t.commit();
        }
        catch (Exception e) {
            t.rollback();
            throw e;
        }
    }

    @GetMapping("/users")
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

    @GetMapping("/users/{username:.+}")
    public ScmUser findUser(@PathVariable("username") String username, Authentication auth) {
        ScmUser user = userRoleRepository.findUserByName(username);
        if (user == null) {
            throw new NotFoundException("User is not found: " + username);
        }

        audit.info(ScmAuditType.USER_DQL, auth, null, 0, "find user by userName=" + username);
        return user;
    }
}
