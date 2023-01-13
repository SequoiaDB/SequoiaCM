package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
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
        // 新增字段，需要在 ScmHdfsDataLocation(BSONObject obj, boolean strict) 增加相应的字段进行校验
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
     * Create a hdfs data location with specified arg.
     *
     * @param obj
     *            a bson containing information about hdfs location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHdfsDataLocation(BSONObject obj, boolean strict) throws ScmInvalidArgumentException {
        this(obj);
        // strict 为 true 时，obj 中不能包含未定义的字段
        // 应与 ScmHdfsDataLocation(BSONObject obj) 中的解析的字段一致，
        // 根据业务需要，部分字段可缺省，但不可以有多余字段
        if (strict) {
            BSONObject objCopy = BsonUtils.deepCopyRecordBSON(obj);
            objCopy.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_HDFS_DFS_ROOT_PATH);

            if (!objCopy.isEmpty()) {
                throw new ScmInvalidArgumentException("contain invalid key:" + objCopy.keySet());
            }
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
