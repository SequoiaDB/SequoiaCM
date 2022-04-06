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

    private ScmShardingType objectShardingType;
    private String bucketName;

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param dataShardingType
     *            data sharding type.
     * @param prefixBucketName
     *            a short prefix to the bucket name.
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType dataShardingType,
            String prefixBucketName) throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(dataShardingType, "dataShardingType");
        checkValueNotNull(prefixBucketName, "prefixBucketName");
        this.shardingType = dataShardingType;
        setPrefixBucketName(prefixBucketName);
    }

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param bucketName
     *            bucket name(A certain bucket that already exists).
     * @param objectShardingType
     *            object sharding type.
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, String bucketName,
            ScmShardingType objectShardingType) throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(bucketName, "bucketName");
        checkValueNotNull(objectShardingType, "objectShardingType");
        this.objectShardingType = objectShardingType;
        this.bucketName = bucketName;
    }

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param dataShardingType
     *            data sharding type.
     * @param prefixBucketName
     *            a short prefix to the bucket name.
     * @param objectShardingType
     *            object sharding type.
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType dataShardingType,
            String prefixBucketName, ScmShardingType objectShardingType)
            throws ScmInvalidArgumentException {
        this(siteName, dataShardingType, prefixBucketName);
        checkValueNotNull(objectShardingType, "objectShardingType");
        this.objectShardingType = objectShardingType;
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

        String objectShardingStr = (String) obj
                .get(FieldName.FIELD_CLWORKSPACE_OBJECT_SHARDING_TYPE);
        if (objectShardingStr != null) {
            ScmShardingType sharding = ScmShardingType.getShardingType(objectShardingStr);
            if (sharding == null) {
                throw new ScmInvalidArgumentException("unknown object sharding type:" + obj);
            }
            setObjectShardingType(sharding);
        }

        String prefix = (String) obj.get(FieldName.FIELD_CLWORKSPACE_CONTAINER_PREFIX);
        if (prefix != null) {
            setPrefixBucketName(prefix);
        }

        String bucketName = (String) obj.get(FieldName.FIELD_CLWORKSPACE_BUCKET_NAME);
        if (bucketName != null) {
            setBucketName(bucketName);
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
        if (prefixBucketName != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_CONTAINER_PREFIX, prefixBucketName);
        }
        if (objectShardingType != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_OBJECT_SHARDING_TYPE,
                    objectShardingType.getName());
        }
        if (bucketName != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_BUCKET_NAME, bucketName);
        }
        return bson;

    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.CEPH_S3;
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

    /**
     * Gets the object sharding type.
     *
     * @return object sharding type.
     */
    public ScmShardingType getObjectShardingType() {
        return objectShardingType;
    }

    /**
     * Sets the object sharding type.
     *
     * @param objectShardingType
     *            object sharding type.
     * @throws ScmInvalidArgumentException
     *             if object sharding type is invalid.
     */
    public void setObjectShardingType(ScmShardingType objectShardingType)
            throws ScmInvalidArgumentException {
        checkValueNotNull(objectShardingType, "objectShardingType");
        this.objectShardingType = objectShardingType;
    }

    /**
     * Gets the bucket name.
     *
     * @return bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the bucket name.
     *
     * @param bucketName
     *            bucket name.
     * @throws ScmInvalidArgumentException
     *             if bucket name is invalid.
     */
    public void setBucketName(String bucketName) throws ScmInvalidArgumentException {
        checkValueNotNull(bucketName, "bucketName");
        this.bucketName = bucketName;
    }
}
