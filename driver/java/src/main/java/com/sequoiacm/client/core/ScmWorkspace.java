package com.sequoiacm.client.core;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.sequoiacm.client.element.bizconf.ScmTagLibMetaOption;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
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
     * Update data locations to the workspace.
     *
     * @param dataLocations
     *            update data location.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateDataLocation(List<ScmDataLocation> dataLocations) throws ScmException;


    /**
     * Update data locations to the workspace.
     *
     * @param dataLocations
     *            update data location.
     * @param mergeTo
     *              true : merge exist data location
     *              false : replace data location
     *              default : true
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateDataLocation(List<ScmDataLocation> dataLocations, boolean mergeTo) throws ScmException;

    /**
     * apply transition to workspace.
     * 
     * @param transitionName
     *            global transition name.
     * @param preferredRegion
     *            priority region,if null,used session's preferred region
     *
     * @param preferredZone
     *            priority zone,if null,used session's preferred zone
     * @return ScmTransitionSchedule
     * @throws ScmException
     */
    public abstract ScmTransitionSchedule applyTransition(String transitionName, String preferredRegion,
            String preferredZone) throws ScmException;

    /**
     * apply transition to workspace.
     * 
     * @param transitionName
     *            global transition name.
     * @return ScmTransitionSchedule
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmTransitionSchedule applyTransition(String transitionName) throws ScmException;

    /**
     * apply transition to workspace.
     *
     * @param transitionName
     *            global transition name.
     *
     * @param transition
     *            custom transition info,if null,direct reference global transition.
     *
     * @param preferredRegion
     *            priority region,if null,used session's preferred region
     *
     * @param preferredZone
     *            priority zone,if null,used session's preferred zone
     * @return ScmTransitionSchedule
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmTransitionSchedule applyTransition(String transitionName, ScmLifeCycleTransition transition,
            String preferredRegion, String preferredZone) throws ScmException;

    /**
     * apply transition to workspace.
     * 
     * @param transitionName
     *            global transition name.
     * @param transition
     *            custom transition info,if null,direct reference global transition.
     * @return ScmTransitionSchedule
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmTransitionSchedule applyTransition(String transitionName, ScmLifeCycleTransition transition)
            throws ScmException;

    /**
     * inport transition by workspace apply transition xml file path.
     *
     * @param xmlPath
     *            workspace apply transition xml file path.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void setTransitionConfig(String xmlPath) throws ScmException;

    /**
     * inport transition by workspace apply transition xml file path.
     * 
     * @param xmlInputStream
     *            xml file input stream
     * @throws ScmException
     *             if error happens.
     */
    public abstract void setTransitionConfig(InputStream xmlInputStream) throws ScmException;

    /**
     * remove transition by transition name.
     *
     * @param transitionName
     *            transition name.
     * @throws ScmException
     *             if error happens.
     */
    public abstract void removeTransition(String transitionName) throws ScmException;

    /**
     * update workspace apply transition info.
     *
     * @param transitionName
     *            transition name.
     *
     * @param transition
     *            new transition info.
     *
     * @param preferredRegion
     *            priority region,if null,used old transition preferred region.
     *
     * @param preferredZone
     *            priority zone,if null,used old transition preferred zone.
     *
     * @return  ScmTransitionScheduleList
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmTransitionSchedule updateTransition(String transitionName, ScmLifeCycleTransition transition,
            String preferredRegion, String preferredZone) throws ScmException;

    public abstract ScmTransitionSchedule updateTransition(String transitionName, ScmLifeCycleTransition transition)
            throws ScmException;

    /**
     * get apply transition info by transition name.
     *
     * @param transitionName
     *            transition name.
     * @return  ScmTransitionSchedule
     * @throws ScmException
     *             if error happens.
     */
    public abstract ScmTransitionSchedule getTransition(String transitionName)throws ScmException;

    /**
     * get workspace apply all transition info.
     *
     * @return  ScmTransitionScheduleList
     * @throws ScmException
     *             if error happens.
     */
    public abstract List<ScmTransitionSchedule> listTransition() throws ScmException;

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
    
    /**
     * Disable the workspace directory.
     * 
     */
    public abstract void disableDirectory() throws ScmException;

    /**
     * Return the strategy to choose site.
     * 
     * @return strategy.
     */
    public abstract String getPreferred();

    /**
     * Update the strategy to choose site.
     * 
     * @param preferred
     *            strategy
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updatePreferred(String preferred) throws ScmException;

    /**
     * Returns the site cache strategy.
     *
     * @return site cache strategy.
     */
    public abstract ScmSiteCacheStrategy getSiteCacheStrategy();

    /**
     * Updates the site cache strategy of workspace.
     * 
     * @param scmSiteCacheStrategy
     *            site cache strategy
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateSiteCacheStrategy(ScmSiteCacheStrategy scmSiteCacheStrategy)
            throws ScmException;

    /**
     * Updates the meta location domain of workspace.
     *
     * @param domainName
     *            domain name
     * @throws ScmException
     *             if error happens.
     */
    public abstract void updateMetaDomain(String domainName) throws ScmException;

    abstract ScmSession getSession();

    abstract int getId();

    abstract void setId(int id);

    abstract BSONObject getExtData();
// 屏蔽标签功能：SEQUOIACM-1411
//    /**
//     * Enable tag retrieval or not.
//     *
//     * @param enableTagRetrieval
//     *            true to enable tag retrieval, false to disable tag retrieval.
//     * @throws ScmException
//     *             if error happens.
//     */
//    public abstract void setEnableTagRetrieval(boolean enableTagRetrieval) throws ScmException;
//
//    /**
//     * Get tag retrieval status.
//     *
//     * @return tag retrieval status.
//     * @throws ScmException
//     *             if error happens.
//     */
//    public abstract ScmWorkspaceTagRetrievalStatus getTagRetrievalStatus() throws ScmException;
//
//    /**
//     * Returns tag lib index task error msg if enable tag retrieval failed, else
//     * return empty string.
//     *
//     * @return error msg.
//     * @throws ScmException
//     *             if error happens.
//     */
//    public abstract String getTagLibIndexErrorMsg() throws ScmException;
//
//    /**
//     * Get tag lib meta option.
//     *
//     * @return tag lib meta option.
//     * @throws ScmException
//     *             if error happens.
//     */
//    public abstract ScmTagLibMetaOption getTagLibMetaOption() throws ScmException;
}
