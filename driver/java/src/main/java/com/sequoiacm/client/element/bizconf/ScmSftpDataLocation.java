package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.infrastructure.common.BsonUtils;
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
        // 新增字段，需要在 ScmSftpDataLocation(BSONObject obj, boolean strict) 增加相应的字段进行校验
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
     * Create a sftp data location with specified arg.
     *
     * @param obj
     *            a bson containing information about sftp location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSftpDataLocation(BSONObject obj, boolean strict) throws ScmInvalidArgumentException {
        this(obj);
        // strict 为 true 时，obj 中不能包含未定义的字段
        // 应与 ScmSftpDataLocation(BSONObject obj) 中的解析的字段一致，
        // 根据业务需要，部分字段可缺省，但不可以有多余字段
        if (strict) {
            BSONObject objCopy = BsonUtils.deepCopyRecordBSON(obj);
            objCopy.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_DATA_PATH);

            if (!objCopy.isEmpty()) {
                throw new ScmInvalidArgumentException("contain invalid key:" + objCopy.keySet());
            }
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
