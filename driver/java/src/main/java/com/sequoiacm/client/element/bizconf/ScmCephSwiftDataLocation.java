package com.sequoiacm.client.element.bizconf;

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
