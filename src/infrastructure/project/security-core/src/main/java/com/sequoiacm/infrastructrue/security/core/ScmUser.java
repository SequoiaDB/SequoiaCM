package com.sequoiacm.infrastructrue.security.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

public class ScmUser implements UserDetails, CredentialsContainer {

    /**
     *
     */
    private static final long serialVersionUID = -9024061686132278587L;

    public static final String JSON_FIELD_USER_ID = "user_id";
    public static final String JSON_FIELD_USERNAME = "username";
    public static final String JSON_FIELD_PASSWORD_TYPE = "password_type";
    public static final String JSON_FIELD_PASSWORD = "password";
    public static final String JSON_FIELD_ENABLED = "enabled";
    public static final String JSON_FIELD_ROLES = "roles";
    public static final String JSON_FIELD_ACCESS_KEY = "accesskey";

    private String userId;
    private String username;
    private String password;
    private ScmUserPasswordType passwordType;
    private Set<ScmRole> roles;
    private boolean enabled;

    private String secretkey;

    private String accesskey;

    ScmUser() {
    }

    public ScmUser(String userId, String username, String password,
            ScmUserPasswordType passwordType, boolean enabled, Collection<ScmRole> roles,
            String accessKey, String secretKey) {
        if (userId == null || "".equals(userId)) {
            throw new IllegalArgumentException("Cannot pass null or empty userId to constructor");
        }

        if (username == null || "".equals(username)) {
            throw new IllegalArgumentException("Cannot pass null or empty username to constructor");
        }

        if (passwordType == null) {
            throw new IllegalArgumentException("Cannot pass null passwordType to constructor");
        }

        if (passwordType == ScmUserPasswordType.LOCAL
                && (password == null || "".equals(password))) {
            throw new IllegalArgumentException("Cannot pass null or empty password to constructor");
        }

        this.userId = userId;
        this.username = username;
        this.password = password;
        this.passwordType = passwordType;
        this.enabled = enabled;
        if (roles != null && !roles.isEmpty()) {
            this.roles = Collections.unmodifiableSet(sortAuthorities(roles));
        }
        else {
            this.roles = Collections.emptySet();
        }
        this.accesskey = accessKey;
        this.secretkey = secretKey;
    }

    @Override
    public Collection<ScmRole> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    public ScmUserPasswordType getPasswordType() {
        return passwordType;
    }

    public boolean hasRole(String roleName) {
        for (ScmRole role : roles) {
            if (role.getRoleName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(ScmRole role) {
        for (ScmRole r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        password = null;
        secretkey = null;
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof ScmUser) {
            if (!userId.equals(((ScmUser) rhs).userId)) {
                return false;
            }
            Set<ScmRole> rhsRoles = ((ScmUser) rhs).roles;
            if (this.roles == null) {
                return rhsRoles == null;
            }
            return this.roles.equals(rhsRoles);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScmUser{");
        sb.append("UserId: ").append(this.userId).append(", ");
        sb.append("Username: ").append(this.username).append(", ");
        sb.append("PasswordType: ").append(this.passwordType.name()).append(", ");
        sb.append("Password: [PROTECTED], ");
        sb.append("Accesskey: ").append(this.accesskey).append(", ");
        sb.append("Secretkey: [PROTECTED], ");
        sb.append("Enabled: ").append(this.enabled).append(", ");
        sb.append("Roles: [");
        if (null != roles && !roles.isEmpty()) {
            boolean first = true;
            for (GrantedAuthority auth : roles) {
                if (!first) {
                    sb.append(",");
                }
                first = false;

                sb.append(auth);
            }
        }
        sb.append("]");

        return sb.toString();
    }

    private static <T extends GrantedAuthority> SortedSet<T> sortAuthorities(
            Collection<T> authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
        // Ensure array iteration order is predictable (as per
        // UserDetails.getAuthorities() contract and SEC-717)
        SortedSet<T> sortedAuthorities = new TreeSet<>(new AuthorityComparator<T>());

        for (T grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority,
                    "GrantedAuthority list cannot contain any null elements");
            sortedAuthorities.add(grantedAuthority);
        }

        return sortedAuthorities;
    }

    private static class AuthorityComparator<T extends GrantedAuthority>
            implements Comparator<T>, Serializable {
        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        @Override
        public int compare(T g1, T g2) {
            // Neither should ever be null as each entry is checked before
            // adding it to
            // the set.
            // If the authority is null, it is a custom authority and should
            // precede
            // others.
            if (g2.getAuthority() == null) {
                return -1;
            }

            if (g1.getAuthority() == null) {
                return 1;
            }

            return g1.getAuthority().compareTo(g2.getAuthority());
        }
    }

    public String getAccesskey() {
        return accesskey;
    }

    public String getSecretkey() {
        return secretkey;
    }

    public static ScmUserBuilder withUsername(String username) {
        return new ScmUserBuilder().username(username);
    }

    public static ScmUserBuilder copyFrom(ScmUser user) {
        return new ScmUserBuilder(user);
    }

    /**
     * Builds the user to be added. At minimum the userId, username, userType,
     * password should provided. The remaining attributes have reasonable
     * defaults.
     */
    public static class ScmUserBuilder {
        private String userId;
        private String username;
        private String password;
        private ScmUserPasswordType passwordType;
        private List<ScmRole> authorities;
        private boolean disabled;
        private String accesskey;
        private String secretkey;

        private ScmUserBuilder() {
        }

        private ScmUserBuilder(ScmUser src) {
            userId = src.userId;
            username = src.username;
            password = src.password;
            passwordType = src.passwordType;
            authorities = new ArrayList<>(src.roles);
            disabled = !src.enabled;
            accesskey = src.accesskey;
            secretkey = src.secretkey;
        }

        private ScmUserBuilder username(String username) {
            Assert.notNull(username, "username cannot be null");
            this.username = username;
            return this;
        }

        public ScmUserBuilder userId(String userId) {
            Assert.notNull(userId, "userId cannot be null");
            this.userId = userId;
            return this;
        }

        public ScmUserBuilder password(String password) {
            Assert.notNull(password, "password cannot be null");
            this.password = password;
            return this;
        }

        public ScmUserBuilder passwordType(ScmUserPasswordType passwordType) {
            Assert.notNull(passwordType, "passwordType cannot be null");
            this.passwordType = passwordType;
            return this;
        }

        public ScmUserBuilder roles(ScmRole... roles) {
            List<ScmRole> authorities = new ArrayList<>(roles.length);
            for (ScmRole role : roles) {
                Assert.isTrue(role.getRoleName().startsWith("ROLE_"),
                        role.getRoleName() + " should start with 'ROLE_'");
                authorities.add(role);
            }
            return roles(authorities);
        }

        public ScmUserBuilder roles(Collection<ScmRole> authorities) {
            this.authorities = new ArrayList<>(authorities);
            return this;
        }

        /**
         * Defines if the account is disabled or not. Default is false.
         *
         * @param disabled
         *            true if the account is disabled, false otherwise
         * @return the {@link ScmUserBuilder} for method chaining (i.e. to
         *         populate additional attributes for this user)
         */
        public ScmUserBuilder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public ScmUserBuilder accesskey(String accesskey) {
            this.accesskey = accesskey;
            return this;
        }

        public ScmUserBuilder secretkey(String secretkey) {
            this.secretkey = secretkey;
            return this;
        }

        public ScmUser build() {
            return new ScmUser(userId, username, password, passwordType, !disabled, authorities,
                    accesskey, secretkey);
        }
    }
}
