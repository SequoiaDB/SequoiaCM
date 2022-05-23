package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import org.bson.BSONObject;

/**
 * Sftp data location.
 */
public class ScmSftpDataLocation extends ScmDataLocation {

    private String dataPath;
    private ScmShardingType dataShardingType;

    /**
     * Create sftp data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param dataPath
     *            data storage path, must be an absolute path.
     * @param dataShardingType
     *            data sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSftpDataLocation(String siteName, String dataPath, ScmShardingType dataShardingType)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(dataPath, "dataPath");
        checkValueNotNull(dataShardingType, "dataShardingType");
        if (!dataPath.startsWith("/")) {
            throw new ScmInvalidArgumentException(
                    "dataPath must be an absolute path, dataPath=" + dataPath);
        }
        this.dataPath = dataPath;
        this.dataShardingType = dataShardingType;
    }

    /**
     * Create sftp data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSftpDataLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Create a sftp data location with specified arg.
     *
     * @param obj
     *            a bson containing information about sftp location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSftpDataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        String shardingStr = (String) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (shardingStr != null) {
            ScmShardingType sharding = ScmShardingType.getShardingType(shardingStr);
            if (sharding == null) {
                throw new ScmInvalidArgumentException("unknown sharding type:" + obj);
            }
            setShardingType(sharding);
        }

        String dataPath = (String) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_PATH);
        if (dataPath != null) {
            setDataPath(dataPath);
        }
    }

    /**
     * Sets the data storage path
     *
     * @param dataPath
     *            data storage path.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setDataPath(String dataPath) throws ScmInvalidArgumentException {
        checkValueNotNull(dataPath, "dataPath");
        this.dataPath = dataPath;
    }

    /**
     * Sets the data sharding type
     *
     * @param dataShardingType
     *            data sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setShardingType(ScmShardingType dataShardingType)
            throws ScmInvalidArgumentException {
        checkValueNotNull(dataShardingType, "dataShardingType");
        this.dataShardingType = dataShardingType;
    }

    /**
     * Gets the data storage path.
     *
     * @return data storage path.
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     * Gets the data sharding type.
     *
     * @return data sharding type.
     */
    public ScmShardingType getDataShardingType() {
        return dataShardingType;
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bson = super.getBSONObject();
        if (dataShardingType != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE, dataShardingType.getName());
        }
        if (dataPath != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_PATH, dataPath);
        }
        return bson;
    }

    @Override
    public ScmType.DatasourceType getType() {
        return ScmType.DatasourceType.SFTP;
    }
}
