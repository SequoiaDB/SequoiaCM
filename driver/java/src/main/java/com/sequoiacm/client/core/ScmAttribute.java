package com.sequoiacm.client.core;

import java.util.Date;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttrRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;

/**
 * The interface of ScmAttribute
 */
public interface ScmAttribute {

    /**
     * Returns the value of the AttributeId property.
     *
     * @return Attribute id.
     */
    ScmId getId();

    /**
     * Returns the value of the AttributeName property.
     *
     * @return Attribute name
     */
    String getName();

    /**
     * Returns the display name.
     *
     * @return display nane.
     */
    String getDisplayName();

    /**
     * Sets the display name.
     *
     * @param displayName
     *            display name.
     * @throws ScmException
     *             if error happens.
     */
    void setDisplayName(String displayName) throws ScmException;

    /**
     * Returns the type.
     *
     * @return type.
     */
    AttributeType getType();

    /**
     * Returns the check rule.
     *
     * @return check rule.
     */
    ScmAttrRule getCheckRule();

    /**
     * Sets the check rule.
     *
     * @param rule
     *            rule
     * @throws ScmException
     *             if error happens.
     */
    void setCheckRule(ScmAttrRule rule) throws ScmException;

    /**
     * Whether the attribute is required.
     *
     * @return true or false.
     */
    boolean isRequired();

    /**
     * Sets the attribute is required.
     *
     * @param required
     *            true or false.
     * @throws ScmException
     *             if error happens.
     */
    void setRequired(boolean required) throws ScmException;

    /**
     * Returns the value of the description property.
     *
     * @return description
     */
    String getDescription();

    /**
     * Set Or Update the value of the description property.
     *
     * @param desc
     *            Attribute description.
     * @throws ScmException
     *             If error happens.
     */
    void setDescription(String desc) throws ScmException;

    /**
     * Returns the value of the User property.
     *
     * @return Create Username
     */
    String getCreateUser();

    /**
     * Returns the value of the CreateTime property.
     *
     * @return CreateTime.
     */
    Date getCreateTime();

    /**
     * Returns the value of the update user property.
     *
     * @return Update Username
     */
    String getUpdateUser();

    /**
     * Returns the value of the UpdateTime property.
     *
     * @return UpdateTime.
     */
    Date getUpdateTime();

    /**
     * Delete a attribute.
     *
     * @throws ScmException
     *             If error happens
     */
    void delete() throws ScmException;

    /**
     * Return the value of the Workspace property.
     *
     * @return workspace
     */
    ScmWorkspace getWorkspace();

    /**
     * Whether the attribute record exist
     *
     * @return true or false
     */
    boolean isExist();
}
