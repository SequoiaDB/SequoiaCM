package com.sequoiacm.client.core;

import java.util.Date;
import java.util.List;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;

/**
 * The interface of ScmClass
 */
public interface ScmClass {

    /**
     * Returns the value of the ClassId property.
     *
     * @return Class id.
     */
    ScmId getId();

    /**
     * Returns the value of the ClassName property.
     *
     * @return Class name
     */
    String getName();
    
    /**
     * Set Or Update the value of the ClassName property.
     *
     * @param className
     *            Class name.
     * @throws ScmException
     *             If error happens.
     */
    void setName(String className) throws ScmException;
    
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
     *            Class description.
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
     * Gets the class attribute list.
     *
     * @return attribute list.
     */
    List<ScmAttribute> listAttrs();
    
    /**
     * Attach the attr to the class.
     *
     * @param attrId
     *            The attribute id to be attach.
     * @throws ScmException
     *             If error happens.
     */
    void attachAttr(ScmId attrId) throws ScmException;
    
    /**
     * Detach the attr from the class.
     *
     * @param attrId
     *            The attribute id to be detach.
     * @throws ScmException
     *             If error happens.
     */
    void detachAttr(ScmId attrId) throws ScmException;
    
    /**
     * Delete a class.
     *
     * @throws ScmException
     *             If error happens
     */
    void delete() throws ScmException;
    
    /**
     * Return the value of the Workspace property.
     * @return workspace
     */
    ScmWorkspace getWorkspace();
    
    /**
     * Whether the class record exist
     * @return true or false
     */
    boolean isExist();
}
