package com.sequoiacm.client.core;

import java.util.Collection;

/**
 * SCM User
 */
public interface ScmUser {

    /**
     * Get id of the user.
     * @return user id
     */
    String getUserId();

    /**
     * Get name of the user.
     * @return username
     */
    String getUsername();

    /**
     * Get password type of the user.
     * @return password type
     */
    ScmUserPasswordType getPasswordType();

    /**
     * Is the user enabled or not.
     * @return true if the user is enabled, otherwise false
     */
    boolean isEnabled();

    /**
     * Get roles the user owned.
     * @return roles
     */
    Collection<ScmRole> getRoles();

    /**
     * Is the user owns the specified role
     * @param role the specified role
     * @return true if the user owns the specified role, otherwise false
     */
    boolean hasRole(ScmRole role);

    /**
     * Is the user owns the specified role
     * @param roleName the specified role name
     * @return true if the user owns the specified role, otherwise false
     */
    boolean hasRole(String roleName);
}
