package com.sequoiacm.client.core;

import java.util.Date;
import java.util.List;

import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;

/**
 * The interface of ScmBatch
 */
public interface ScmBatch {

    /**
     * Returns the value of the BatchId property.
     *
     * @return Batch id.
     */
    ScmId getId();

    /**
     * Returns the value of the BatchName property.
     *
     * @return Batch name
     */
    String getName();

    /**
     * Set Or Update the value of the BatchName property.
     *
     * @param name
     *            Batch name.
     * @throws ScmException
     *             If error happens.
     */
    void setName(String name) throws ScmException;

    /**
     * Returns the value of the Batch ClassId property.
     *
     * @return Batch class id.
     */
    ScmId getClassId();

    /**
     * Returns the value of the User property.
     *
     * @return Username
     */
    String getCreateUser();

    /**
     * Returns the value of the CreateTime property.
     *
     * @return CreateTime.
     */
    Date getCreateTime();

    /**
     * Returns the value of the UpdateUser property.
     *
     * @return UpdateUser.
     */
    String getUpdateUser();

    /**
     * Returns the value of the UpdateTime property.
     *
     * @return UpdateTime
     */
    Date getUpdateTime();

    /**
     * Return the value of the Workspace property name.
     *
     * @return Workspace name
     */
    String getWorkspaceName();

    /**
     * Returns the value of the Properties property.
     *
     * @return Batch class properties
     */
    ScmClassProperties getClassProperties();

    /**
     * Set or Update the single class property.
     * 
     * @param key
     *            key
     * @param value
     *            value
     * @throws ScmException
     *             If error happens.
     */
    void setClassProperty(String key, Object value) throws ScmException;

    /**
     * Sets or Updates the value of the Class and Properties.
     * 
     * @param properties
     *            Batch properties.
     * @throws ScmException
     *             If error happens.
     * 
     */
    void setClassProperties(ScmClassProperties properties) throws ScmException;

    /**
     * Returns the value of the Tags property.
     *
     * @return Batch custom tags
     */
    ScmTags getTags();

    /**
     * Sets or Updates the value of the Tags property.
     *
     * <br>
     * <br>
     * <b>Note:</b> <br>
     * When you update the properties, {@link #getTags()} is called first and
     * update on original properties.
     * 
     * @param tags
     *            Batch tags.
     * @throws ScmException
     *             If error happens.
     */
    void setTags(ScmTags tags) throws ScmException;

    /**
     * add the value of the Tags property.
     * 
     * @param tag
     *            Batch tag.
     * @throws ScmException
     *             If error happens.
     */
    void addTag(String tag) throws ScmException;

    /**
     * remove the value of the Tags property.
     *
     * 
     * @param tag
     *            Batch tag.
     * @throws ScmException
     *             If error happens.
     */
    void removeTag(String tag) throws ScmException;

    /**
     * Gets the batch file list.
     *
     * @return file list.
     * @throws ScmException
     *             If error happens.
     */
    List<ScmFile> listFiles() throws ScmException;

    /**
     * Save the batch and do not associate the file. It only be invoked by a new
     * batch instance.
     *
     * @return The value of the BatchId property.
     * @throws ScmException
     *             If error happens
     */
    ScmId save() throws ScmException;

    /**
     * Delete a batch.
     *
     * @throws ScmException
     *             If error happens
     */
    void delete() throws ScmException;

    /**
     * Attach the file to the batch.
     *
     * @param fileId
     *            The file id to be attach.
     * @throws ScmException
     *             If error happens.
     */
    void attachFile(ScmId fileId) throws ScmException;

    /**
     * Detach the file from the batch.
     *
     * @param fileId
     *            The file id to be detach.
     * @throws ScmException
     *             If error happens.
     */
    void detachFile(ScmId fileId) throws ScmException;
}
