package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;

/**
 * Ceph s3 data location class
 */
public class ScmCephS3DataLocation extends ScmDataLocation {
    private ScmShardingType shardingType;
    private String prefixBucketName;

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param shardingType
     *            data sharding type.
     * @param prefixBucketName
     *            a short prefix to the bucket name.
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType shardingType,
            String prefixBucketName) throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(shardingType, "shardingType");
        checkValueNotNull(prefixBucketName, "prefixBucketName");
        this.shardingType = shardingType;
        setPrefixBucketName(prefixBucketName);
    }

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephS3DataLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param obj
     *            a bson containing information about ceph s3 location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephS3DataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        String shardingStr = (String) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (shardingStr != null) {
            ScmShardingType sharding = ScmShardingType.getShardingType(shardingStr);
            if (sharding == null) {
                throw new ScmInvalidArgumentException("unknown sharding type:" + obj);
            }
            setShardingType(sharding);
        }

        String prefix = (String) obj.get(FieldName.FIELD_CLWORKSPACE_CONTAINER_PREFIX);
        if (prefix != null) {
            setPrefixBucketName(prefix);
        }
    }

    /**
     * Gets the data sharding type.
     *
     * @return sharding type.
     */
    public ScmShardingType getShardingType() {
        return shardingType;
    }

    /**
     * Sets the data sharding type.
     *
     * @param shardingType
     *            sharding type.
     * @throws ScmInvalidArgumentException
     *             if sharding type is invalid.
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

    /**
     * Gets the prefix of bucket name.
     *
     * @return prefix.
     */
    public String getPrefixBucketName() {
        return prefixBucketName;
    }

    /**
     * Sets the prefix of bucket name.
     *
     * @param prefixBucketName
     *            prefix.
     * @throws ScmInvalidArgumentException
     *             if prefix is invalid.
     */
    public void setPrefixBucketName(String prefixBucketName) throws ScmInvalidArgumentException {
        checkValueNotNull(prefixBucketName, "prefixBucketName");
        this.prefixBucketName = prefixBucketName;
    }

}
