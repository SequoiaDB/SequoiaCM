package com.sequoiacm.client.core;

import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.exception.ScmException;

/**
 * SCM privilege.
 *
 */
public interface ScmPrivilege {
    /**
     * Get the id of the privilege.
     *
     * @return privilege id.
     */
    public String getId();

    /**
     * Get the role type of the privilege.
     *
     * @return role type.
     */
    public String getRoleType();

    /**
     * Get the role id of the privilege.
     *
     * @return role id.
     */
    public String getRoleId();

    /**
     * Get the resource id of the privilege.
     *
     * @return resource id.
     */
    public String getResourceId();

    /**
     * Get the role of the privilege.
     *
     * @return role instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmRole getRole() throws ScmException;

    /**
     * Get the resource of the privilege.
     *
     * @return resource instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmResource getResource() throws ScmException;

    /**
     * Get the privilege type of the privilege.
     *
     * @return privilege type string.
     */
    @Deprecated
    public String getPrivilege();

    /**
     * Get the privilege type of the privilege.
     *
     * @return privilege type.
     */
    public ScmPrivilegeType getPrivilegeType();
}
