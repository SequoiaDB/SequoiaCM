package com.sequoiacm.infrastructrue.security.core;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ScmRole implements GrantedAuthority {
    /**
     *
     */
    private static final long serialVersionUID = 8670015815272182516L;

    public static final String JSON_FIELD_ROLE_ID = "role_id";
    public static final String JSON_FIELD_ROLE_NAME = "role_name";
    public static final String JSON_FIELD_DESCRIPTION = "description";

    public static final String ROLE_NAME_PREFIX = "ROLE_";
    public static final String AUTH_ADMIN_SHORT_NAME = "AUTH_ADMIN";
    public static final String AUTH_ADMIN_ROLE_NAME = ROLE_NAME_PREFIX + AUTH_ADMIN_SHORT_NAME;

    public static final String AUTH_MONITOR_SHORT_NAME = "AUTH_MONITOR";
    public static final String AUTH_MONITOR_ROLE_NAME = ROLE_NAME_PREFIX + AUTH_MONITOR_SHORT_NAME;

    private String roleId;
    private String roleName;
    private String description;

    ScmRole() {
    }

    public ScmRole(String roleId, String roleName, String description) {
        if (roleId == null || "".equals(roleId)) {
            throw new IllegalArgumentException("Cannot pass null or empty roleId to constructor");
        }

        if (roleName == null || "".equals(roleName)) {
            throw new IllegalArgumentException("Cannot pass null or empty roleName to constructor");
        }

        if (!roleName.startsWith(ROLE_NAME_PREFIX)) {
            throw new IllegalArgumentException(
                    "Role name should start with '" + ROLE_NAME_PREFIX + "'");
        }

        this.roleId = roleId;
        this.roleName = roleName;
        if (StringUtils.hasText(description)) {
            this.description = description;
        }
        else {
            this.description = "";
        }
    }

    @Override
    public String getAuthority() {
        return roleName;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAuthAdmin() {
        return roleName.equals(AUTH_ADMIN_ROLE_NAME);
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof ScmRole) {
            return roleId.equals(((ScmRole) rhs).roleId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return roleId.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScmRole{");
        sb.append("RoleId: ").append(this.roleId).append(", ");
        sb.append("RoleName: ").append(this.roleName).append(", ");
        sb.append("Description: ").append(this.description).append("}");

        return sb.toString();
    }

    public static ScmRoleBuilder withRoleName(String roleName) {
        return new ScmRoleBuilder().roleName(roleName);
    }

    /**
     * Builds the role to be added. The roleId, roleName should provided.
     */
    public static class ScmRoleBuilder {
        private String roleId;
        private String roleName;
        private String description;

        private ScmRoleBuilder() {
        }

        private ScmRoleBuilder roleName(String roleName) {
            Assert.notNull(roleName, "roleName cannot be null");
            Assert.isTrue(roleName.startsWith(ROLE_NAME_PREFIX),
                    "roleName should start with " + ROLE_NAME_PREFIX);
            this.roleName = roleName;
            return this;
        }

        public ScmRoleBuilder roleId(String roleId) {
            Assert.notNull(roleId, "roleId cannot be null");
            this.roleId = roleId;
            return this;
        }

        public ScmRoleBuilder description(String desc) {
            if (StringUtils.hasText(desc)) {
                this.description = desc;
            }
            return this;
        }

        public ScmRole build() {
            return new ScmRole(roleId, roleName, description);
        }
    }
}
