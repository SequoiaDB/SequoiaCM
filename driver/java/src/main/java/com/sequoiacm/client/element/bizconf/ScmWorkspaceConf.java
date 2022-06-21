package com.sequoiacm.client.element.bizconf;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.ScmShardingType;

/**
 * Workspace config class.
 */
public class ScmWorkspaceConf {

    private String name;
    private String description;
    private ScmMetaLocation metaLocation;
    private List<ScmDataLocation> dataLocations;
    private ScmShardingType batchShardingType;
    private String batchIdTimeRegex;
    private String batchIdTimePattern;
    private boolean batchFileNameUnique;
    private boolean enableDirectory = false;

    /**
     * Create a empty config instance.
     */
    public ScmWorkspaceConf() {
    }

    /**
     * Create a config with specified args.
     *
     * @param name
     *            workspace name.
     * @param metaLocation
     *            meta location.
     * @param dataLocations
     *            data locations.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmWorkspaceConf(String name, ScmMetaLocation metaLocation,
            List<ScmDataLocation> dataLocations) throws ScmInvalidArgumentException {
        if (!ScmArgChecker.Workspace.checkWorkspaceName(name)) {
            throw new ScmInvalidArgumentException("invalid workspace name:name=" + name);
        }

        if (metaLocation == null) {
            throw new ScmInvalidArgumentException("metalocation is null");
        }

        if (dataLocations == null) {
            throw new ScmInvalidArgumentException("dataLocations is null");
        }

        checkNullEle(dataLocations);

        this.name = name;
        this.metaLocation = metaLocation;
        this.dataLocations = dataLocations;
    }

    /**
     * Create a config with specified args.
     *
     * @param name
     *            workspace name.
     * @param metaLocation
     *            meta location.
     * @param dataLocations
     *            data locations
     * @param description
     *            workspace description.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmWorkspaceConf(String name, ScmMetaLocation metaLocation,
            List<ScmDataLocation> dataLocations, String description)
            throws ScmInvalidArgumentException {
        this(name, metaLocation, dataLocations);
        if (description == null) {
            throw new ScmInvalidArgumentException("descriptions is null");
        }
        this.description = description;
    }

    /**
     * Gets the workspace name.
     *
     * @return workspace name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the workspace name.
     *
     * @param name
     *            workspace name.
     * @return current instance.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmWorkspaceConf setName(String name) throws ScmInvalidArgumentException {
        if (!ScmArgChecker.Workspace.checkWorkspaceName(name)) {
            throw new ScmInvalidArgumentException("invalid workspace name:name=" + name);
        }
        this.name = name;
        return this;
    }

    /**
     * Gets the meta location.
     *
     * @return meta location.
     */
    public ScmMetaLocation getMetaLocation() {
        return metaLocation;
    }

    /**
     * Sets the meta location.
     *
     * @param metaLocation
     *            meta location.
     * @return current instance.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmWorkspaceConf setMetaLocation(ScmMetaLocation metaLocation)
            throws ScmInvalidArgumentException {
        if (metaLocation == null) {
            throw new ScmInvalidArgumentException("metalocation is null");
        }
        this.metaLocation = metaLocation;
        return this;
    }

    /**
     * Gets the data locations.
     *
     * @return data locations.
     */
    public List<ScmDataLocation> getDataLocations() {
        return dataLocations;
    }

    /**
     * Sets the data locations.
     *
     * @param dataLocations
     *            data locations.
     * @return current instance.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmWorkspaceConf setDataLocations(List<ScmDataLocation> dataLocations)
            throws ScmInvalidArgumentException {
        if (dataLocations == null) {
            throw new ScmInvalidArgumentException("datalocations is null");
        }
        checkNullEle(dataLocations);
        this.dataLocations = dataLocations;
        return this;
    }

    /**
     * Adds a data location.
     *
     * @param dataLocation
     *            data location.
     * @return current instance.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmWorkspaceConf addDataLocation(ScmDataLocation dataLocation)
            throws ScmInvalidArgumentException {
        if (dataLocation == null) {
            throw new ScmInvalidArgumentException("datalocation is null");
        }
        if (dataLocations == null) {
            dataLocations = new ArrayList<ScmDataLocation>();
        }
        dataLocations.add(dataLocation);
        return this;
    }

    /**
     * Gets the bson object.
     *
     * @return BSON.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public BSONObject getBSONObject() throws ScmInvalidArgumentException {
        BasicBSONObject bson = new BasicBSONObject();
        if (name == null) {
            throw new ScmInvalidArgumentException("name is null");
        }
        bson.put(FieldName.FIELD_CLWORKSPACE_NAME, getName());

        if (metaLocation == null) {
            throw new ScmInvalidArgumentException("metalocation is null");
        }
        bson.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION, metaLocation.getBSONObject());

        if (dataLocations == null) {
            throw new ScmInvalidArgumentException("datalocations is null");
        }
        BasicBSONList listBson = new BasicBSONList();
        for (ScmDataLocation dataLocation : dataLocations) {
            listBson.add(dataLocation.getBSONObject());
        }
        bson.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, listBson);
        if (description != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DESCRIPTION, description);
        }
        bson.put(FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE, batchFileNameUnique);
        if (batchShardingType != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE, batchShardingType.getName());
        }
        if (batchIdTimePattern != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN, batchIdTimePattern);
        }
        if (batchIdTimeRegex != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX, batchIdTimeRegex);
        }
        bson.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, enableDirectory);
        return bson;
    }

    /**
     * Gets the description.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description
     *            description
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setDescription(String description) throws ScmInvalidArgumentException {
        if (description == null) {
            throw new ScmInvalidArgumentException("descriptions is null");
        }
        this.description = description;
    }

    private void checkNullEle(List<ScmDataLocation> l) throws ScmInvalidArgumentException {
        for (ScmDataLocation o : l) {
            if (o == null) {
                throw new ScmInvalidArgumentException("datalocations contains null element");
            }
        }
    }

    /**
     * Returns the sharding type of batch.
     * 
     * @return batch sharding type.
     */
    public ScmShardingType getBatchShardingType() {
        return batchShardingType;
    }

    /**
     * Set the sharding type of batch.
     * 
     * @param batchShardingType
     *            batch sharding type.
     */
    public void setBatchShardingType(ScmShardingType batchShardingType) {
        this.batchShardingType = batchShardingType;
    }

    /**
     * Returns the time regex of batch id.
     * 
     * @return regex.
     */
    public String getBatchIdTimeRegex() {
        return batchIdTimeRegex;
    }

    /**
     * Set the time regex of batch id.
     * 
     * @param batchIdTimeRegex
     *            regex.
     */
    public void setBatchIdTimeRegex(String batchIdTimeRegex) {
        this.batchIdTimeRegex = batchIdTimeRegex;
    }

    /**
     * Returns the time pattern of batch id.
     * 
     * @return time pattern.
     */
    public String getBatchIdTimePattern() {
        return batchIdTimePattern;
    }

    /**
     * Set the time pattern of batch id.
     * 
     * @param batchIdTimePattern
     *            time pattern.
     */
    public void setBatchIdTimePattern(String batchIdTimePattern) {
        this.batchIdTimePattern = batchIdTimePattern;
    }

    /**
     * Return true if the file name is unique in a batch, elase return false.
     * 
     * @return return true if the file name is unique in a batch.
     */
    public boolean isBatchFileNameUnique() {
        return batchFileNameUnique;
    }

    /**
     * Set the file name is unique or not in a batch.
     * 
     * @param batchFileNameUnique
     *            is unique.
     */
    public void setBatchFileNameUnique(boolean batchFileNameUnique) {
        this.batchFileNameUnique = batchFileNameUnique;
    }

    /**
     * Return true if the workspace enable directory feature, else return false.
     * 
     * @return return true if the workspace enable directory feature.
     */
    public boolean isEnableDirectory() {
        return enableDirectory;
    }

    /**
     * Enable the directory feature or not.
     * 
     * @param enableDirectory
     *            is enable or not.
     */
    public void setEnableDirectory(boolean enableDirectory) {
        this.enableDirectory = enableDirectory;
    }
}
