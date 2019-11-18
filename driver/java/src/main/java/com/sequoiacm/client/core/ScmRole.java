package com.sequoiacm.client.core;

/**
 * SCM Role
 */
public interface ScmRole {

    /**
     * Get the id of the role.
     * @return role id
     */
    String getRoleId();

    /**
     * Get the name of the role.
     * @return role name
     */
    String getRoleName();

    /**
     * Get the description of the role.
     * @return description
     */
    String getDescription();
}
