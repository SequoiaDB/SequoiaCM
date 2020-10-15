package com.sequoiacm.client.core;

import java.util.Date;
import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;

/**
 * The interface to operate ScmWorkspace object.
 *
 * @since 2.1
 */
public abstract class ScmWorkspace {

    /**
     * Returns the value of the Name property.
     *
     * @return Workspace name.
     * @since 2.1
     */
    public abstract String getName();

    /**
     * Gets the meta location of the workspace.
     *
     * @return meta location.
     */
    public abstract ScmMetaLocation getMetaLocation();

    /**
     * Gets the data locations of the workspace.
     *
     * @return data locations.
     */
    public abstract List<ScmDataLocation> getDataLocations();

    /**
     * Gets the descriptions of the workspace.
     *
     * @return description.
     */
    public abstract String getDescription();

    /**
     * Gets the created user of the workspace.
     *
     * @return created user.
     */
    public abstract String getCreateUser();

    /**
     * Gets the updated user of the workspace.
     *
     * @return last updated user.
     */
    public abstract String getUpdateUser();

    /**
     * Gets the updated time of the workspace.
     *
     * @return last updated time.
     */
    public abstract Date getUpdateTime();

    /**
     * Gets the created time of the workspace.
     *
     * @return created time.
     */
    public abstract Date getCreateTime();

    /**
     * Updates the description of the workspace.
     *
     * @param newDescription
     *            new description.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updatedDescription(String newDescription) throws ScmException;

    /**
     * Adds a new data location to the workspace.
     *
     * @param dataLocation
     *            new data location.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void addDataLocation(ScmDataLocation dataLocation) throws ScmException;

    /**
     * remove a specified data location from the workspace.
     *
     * @param siteName
     *            site name.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void removeDataLocation(String siteName) throws ScmException;

    /**
     * Returns the sharding type of batch.
     * 
     * @return batch sharding type.
     */
    public abstract ScmShardingType getBatchShardingType();

    /**
     * Returns the time regex of batch id.
     * 
     * @return regex.
     */
    public abstract String getBatchIdTimeRegex();

    /**
     * Returns the time pattern of batch id.
     * 
     * @return time pattern.
     */
    public abstract String getBatchIdTimePattern();

    /**
     * Return true if the file name is unique in a batch, else return false.
     * 
     * @return return true if the file name is unique in a batch.
     */
    public abstract boolean isBatchFileNameUnique();

    /**
     * Return true if the workspace enable directory feature, else return false.
     * 
     * @return return true if the workspace enable directory feature.
     */
    public abstract boolean isEnableDirectory();

    abstract ScmSession getSession();

    abstract int getId();

    abstract void setId(int id);

    abstract BSONObject getExtData();
}
