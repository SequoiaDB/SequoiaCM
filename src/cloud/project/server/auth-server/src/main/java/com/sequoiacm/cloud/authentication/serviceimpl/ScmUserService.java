package com.sequoiacm.cloud.authentication.serviceimpl;

import com.sequoiacm.cloud.authentication.config.TokenConfig;
import com.sequoiacm.cloud.authentication.dao.IPrivVersionDao;
import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.cloud.authentication.exception.ForbiddenException;
import com.sequoiacm.cloud.authentication.exception.NotFoundException;
import com.sequoiacm.cloud.authentication.service.IUserService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.Bcrypt;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.session.data.sequoiadb.SequoiadbSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

@Service
public class ScmUserService implements IUserService {
    private static final Logger logger = LoggerFactory.getLogger(ScmUserService.class);

    @Autowired
    private ScmUserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SequoiadbSessionRepository sessionRepository;

    @Autowired
    private SaltSource saltSource;

    @Autowired
    private ScmAudit audit;

    @Autowired
    private ITransaction transactionFactory;

    @Autowired
    private IPrivVersionDao versionDao;

    @Autowired
    private TokenConfig tokenConfig;

    @Override
    public ScmUser alterUser(Authentication authentication, String username, String oldPassword,
            String newPassword, ScmUserPasswordType passwordType, List<String> addRoles,
            List<String> delRoles, Boolean enabled, Boolean cleanSessions, boolean needToDecrypt)
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

        ScmUser currentUser = (ScmUser) authentication.getPrincipal();
        boolean isModifyMyself = isSameUser(currentUser, user);
        boolean IMAdmin = isAdmin(currentUser);
        checkPermission(isModifyMyself, IMAdmin, currentUser, passwordType, addRoles, delRoles,
                enabled, cleanSessions);

        boolean checkOldPassword = (isModifyMyself || isAdmin(user));

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

        boolean altered = false;
        boolean securityAltered = false;
        ScmUser.ScmUserBuilder builder = ScmUser.withUsername(username).userId(user.getUserId());
        String message = "alter user : ";
        // process password
        if (StringUtils.hasText(newPassword) && !newPassword.equals(user.getPassword())) {
            if (needToDecrypt) {
                newPassword = ScmPasswordMgr.getInstance()
                        .decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, newPassword);
            }
            if (checkOldPassword) {
                if (!StringUtils.hasText(oldPassword)) {
                    throw new BadRequestException("Missing old password for user: " + username);
                }

                if (needToDecrypt) {
                    oldPassword = ScmPasswordMgr.getInstance()
                            .decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, oldPassword);
                }

                Object salt = saltSource.getSalt(user);
                if (!passwordEncoder.isPasswordValid(user.getPassword(), oldPassword, salt)) {
                    throw new BadRequestException("Incorrect old password for user: " + username);
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
            if (isModifyMyself) {
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
                    if (isModifyMyself & role.isAuthAdmin()) {
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

            if (securityAltered && cleanSessions != null && cleanSessions && !isModifyMyself) {
                sessionRepository.deleteSessions(username);
            }

            audit.info(ScmAuditType.UPDATE_USER, authentication, null, 0, message);
            return newUser;
        }
        else {
            return user;
        }
    }

    @Override
    public ScmUser createUser(String username, String type, String password, Authentication auth,
            boolean needToDecrypt) throws Exception {
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
            if (needToDecrypt) {
                try {
                    password = ScmPasswordMgr.getInstance()
                            .decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password);
                }
                catch (Exception e) {
                    logger.error("Failed to decrypt password! Cannot create user: " + username);
                    throw e;
                }
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

    @Override
    public BSONObject findUserSalt(String username) throws Exception {
        ScmUser user = userRoleRepository.findUserByName(username);
        if (user == null) {
            throw new ScmServerException(ScmError.SALT_NOT_EXIST,
                    "Salt does not exist: " + username);
        }
        if (user.getPasswordType().equals(ScmUserPasswordType.LDAP)
                || user.getPasswordType().equals(ScmUserPasswordType.TOKEN)) {
            throw new ScmServerException(ScmError.FIND_SALT_FAILED,
                    "Can not get salt: " + user.getUsername());
        }
        String bcryptPassword = user.getPassword();
        BasicBSONObject saltAndDate = new BasicBSONObject();
        String salt = Bcrypt.getPureSalt(bcryptPassword).toString();
        saltAndDate.put("Salt", salt);
        saltAndDate.put("Date", parseDateToISO8601Date(new Date()));
        return saltAndDate;
    }

    private void checkPermission(boolean isModifyMyself, boolean IMAdmin, ScmUser currentUser,
            ScmUserPasswordType passwordType, List<String> addRoles, List<String> delRoles,
            Boolean enabled, Boolean cleanSessions) {
        if (!IMAdmin && !isModifyMyself) {
            throw new BadRequestException("No permission to modify other users' information: "
                    + currentUser.getUsername());
        }
        if (!IMAdmin && isModifyMyself) {
            if ((passwordType != null) || (addRoles != null && addRoles.size() > 0)
                    || (delRoles != null && delRoles.size() > 0) || (enabled != null)
                    || (cleanSessions != null)) {
                throw new BadRequestException("Can not modify information other than password: "
                        + currentUser.getUsername());
            }
        }
    }

    private boolean isSameUser(ScmUser currentUser, ScmUser modifiedUser) {
        return modifiedUser.getUsername().equals(currentUser.getUsername());
    }

    private boolean isAdmin(ScmUser currentUser) {
        for (ScmRole role : currentUser.getAuthorities()) {
            if (role.isAuthAdmin()) {
                return true;
            }
        }
        return false;
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

    private String parseDateToISO8601Date(Date serverDate) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return df.format(serverDate);
    }
}
