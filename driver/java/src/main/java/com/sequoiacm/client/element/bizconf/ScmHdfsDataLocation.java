package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;

/**
 * Hdfs data location.
 */
public class ScmHdfsDataLocation extends ScmDataLocation {
    private ScmShardingType shardingType;
    private String rootPath;

    /**
     * Create a hdfs data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param shardingType
     *            data sharding type.
     * @param rootPath
     *            hdfs path.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHdfsDataLocation(String siteName, ScmShardingType shardingType, String rootPath)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(shardingType, "shardingType");
        checkValueNotNull(shardingType, "rootPath");
        this.shardingType = shardingType;
        setRootPath(rootPath);
    }

    /**
     * Create a hdfs data location with specified arg.
     *
     * @param obj
     *            a bson containing information about hdfs location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHdfsDataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        String shardingStr = (String) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (shardingStr != null) {
            ScmShardingType sharding = ScmShardingType.getShardingType(shardingStr);
            if (sharding == null) {
                throw new ScmInvalidArgumentException("unknown sharding type:" + obj);
            }
            setShardingType(sharding);
        }
        String path = (String) obj.get(FieldName.FIELD_CLWORKSPACE_HDFS_DFS_ROOT_PATH);
        if (path != null) {
            setRootPath(path);
        }
    }

    /**
     * Create a hdfs data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHdfsDataLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Gets the data sharding type.
     *
     * @return data sharding type.
     */
    public ScmShardingType getShardingType() {
        return shardingType;
    }

    /**
     * Sets the data sharding type.
     *
     * @param shardingType
     *            data sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setShardingType(ScmShardingType shardingType) throws ScmInvalidArgumentException {
        checkValueNotNull(shardingType, "shardingType");
        this.shardingType = shardingType;
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bson = super.getBSONObject();
        if (shardingType != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE, shardingType.getName());
        }
        if (rootPath != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_HDFS_DFS_ROOT_PATH, rootPath);
        }
        return bson;

    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.HDFS;
    }

    /**
     * Gets the hdfs path.
     *
     * @return path.
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Sets a hdfs path.
     *
     * @param rootPath
     *            path.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setRootPath(String rootPath) throws ScmInvalidArgumentException {
        checkValueNotNull(rootPath, "rootPath");
        this.rootPath = rootPath;
    }

}
