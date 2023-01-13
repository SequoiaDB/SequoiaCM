package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;

/**
 * Cepth swift data location.
 */
public class ScmCephSwiftDataLocation extends ScmDataLocation {
    private ScmShardingType shardingType;

    /**
     * Create ceph swift data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param shardingType
     *            data sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephSwiftDataLocation(String siteName, ScmShardingType shardingType)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(shardingType, "shardingType");
        this.shardingType = shardingType;
    }

    /**
     * Create ceph swift data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephSwiftDataLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Create ceph swift data location with specified arg.
     *
     * @param obj
     *            a bson containing information about ceph swift location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephSwiftDataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        // 新增字段，需要在 ScmCephSwiftDataLocation(BSONObject obj, boolean strict) 增加相应的字段进行校验
        String shardingStr = (String) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (shardingStr != null) {
            ScmShardingType sharding = ScmShardingType.getShardingType(shardingStr);
            if (sharding == null) {
                throw new ScmInvalidArgumentException("unknown sharding type:" + obj);
            }
            setShardingType(sharding);
        }
    }

    /**
     * Create ceph swift data location with specified arg.
     *
     * @param obj
     *            a bson containing information about ceph swift location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephSwiftDataLocation(BSONObject obj, boolean strict)
            throws ScmInvalidArgumentException {
        this(obj);
        // strict 为 true 时，obj 中不能包含未定义的字段
        // 应与 ScmCephSwiftDataLocation(BSONObject obj) 中的解析的字段一致，
        // 根据业务需要，部分字段可缺省，但不可以有多余字段
        if (strict) {
            BSONObject objCopy = BsonUtils.deepCopyRecordBSON(obj);
            objCopy.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);

            if (!objCopy.isEmpty()) {
                throw new ScmInvalidArgumentException("contain invalid key:" + objCopy.keySet());
            }
        }
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
        return bson;

    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.CEPH_SWIFT;
    }

}
