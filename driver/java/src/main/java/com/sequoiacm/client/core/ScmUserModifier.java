package com.sequoiacm.client.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sequoiacm.client.util.Strings;

/**
 * ScmUser Modifier
 */
public class ScmUserModifier {
    private String oldPassword;
    private String newPassword;
    private Set<String> addRoles = new HashSet<String>();
    private Set<String> delRoles = new HashSet<String>();
    private ScmUserPasswordType passwordType;
    private Boolean enabled;
    private Boolean cleanSessions;

    /**
     * Create a user modifier.
     */
    public ScmUserModifier() {
    }

    /**
     * Modifies password.
     *
     * @param oldPassword
     *            old password.
     * @param newPassword
     *            new password.
     * @return the modifier.
     */
    public ScmUserModifier setPassword(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        return this;
    }

    /**
     * Adds a specified role.
     *
     * @param roleName
     *            role name.
     * @return the modifier.
     */
    public ScmUserModifier addRole(String roleName) {
        if (Strings.hasText(roleName)) {
            addRoles.add(roleName);
        }
        return this;
    }

    /**
     * Adds a specified role.
     *
     * @param role
     *            role object.
     * @return the modifier.
     */
    public ScmUserModifier addRole(ScmRole role) {
        if (role != null) {
            addRole(role.getRoleName());
        }
        return this;
    }

    /**
     * Adds a set of roles.
     *
     * @param roleNames
     *            roles.
     * @return the modifier.
     */
    public ScmUserModifier addRoleNames(Collection<String> roleNames) {
        if (roleNames != null) {
            for (String roleName : roleNames) {
                addRole(roleName);
            }
        }
        return this;
    }

    /**
     * Adds a set of roles.
     *
     * @param roles
     *            roles
     * @return the modifier.
     */
    public ScmUserModifier addRoles(Collection<ScmRole> roles) {
        if (roles != null) {
            for (ScmRole role : roles) {
                addRole(role);
            }
        }
        return this;
    }

    /**
     * Delete a specified role.
     *
     * @param roleName
     *            role name.
     * @return the modifier.
     */
    public ScmUserModifier delRole(String roleName) {
        if (Strings.hasText(roleName)) {
            delRoles.add(roleName);
        }
        return this;
    }

    /**
     * Delete a specified role.
     *
     * @param role
     *            role object.
     * @return the modifier.
     */
    public ScmUserModifier delRole(ScmRole role) {
        if (role != null) {
            delRole(role.getRoleName());
        }
        return this;
    }

    /**
     * Delete a set of roles.
     *
     * @param roleNames
     *            roles.
     * @return the modifier.
     */
    public ScmUserModifier delRoleNames(Collection<String> roleNames) {
        if (roleNames != null) {
            for (String roleName : roleNames) {
                delRole(roleName);
            }
        }
        return this;
    }

    /**
     * Delete a set of roles
     *
     * @param roles
     *            roles.
     * @return the modifier.
     */
    public ScmUserModifier delRoles(Collection<ScmRole> roles) {
        if (roles != null) {
            for (ScmRole role : roles) {
                delRole(role);
            }
        }
        return this;
    }

    /**
     * Sets password type.
     *
     * @param passwordType
     *            password type.
     * @return the modifier.
     */
    public ScmUserModifier setPasswordType(ScmUserPasswordType passwordType) {
        this.passwordType = passwordType;
        return this;
    }

    /**
     * Enable or disable user.
     *
     * @param enabled
     *            true or false.
     * @return the modifier.
     */
    public ScmUserModifier setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Clean all sessions of the user.
     *
     * @param cleanSessions
     *            true or false.
     * @return the modifier.
     */
    public ScmUserModifier setCleanSessions(Boolean cleanSessions) {
        this.cleanSessions = cleanSessions;
        return this;
    }

    /**
     * Gets the old password.
     *
     * @return old password.
     */
    public String getOldPassword() {
        return oldPassword;
    }

    /**
     * Gets the new password.
     *
     * @return new password.
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * Gets the new roles.
     *
     * @return roles.
     */
    public Set<String> getAddRoles() {
        return addRoles;
    }

    /**
     * Gets the deleted roles.
     *
     * @return deleted roles.
     */
    public Set<String> getDelRoles() {
        return delRoles;
    }

    /**
     * Gets the password type.
     *
     * @return password type.
     */
    public ScmUserPasswordType getPasswordType() {
        return passwordType;
    }

    /**
     * Is enabled.
     *
     * @return true false.
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Is clean all sessions.
     *
     * @return true or false.
     */
    public Boolean getCleanSessions() {
        return cleanSessions;
    }
}
