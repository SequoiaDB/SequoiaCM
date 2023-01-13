package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

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

    private ScmCephS3UserConfig primaryConfig;

    private ScmCephS3UserConfig standbyConfig;

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
     * @param dataShardingType
     *            data sharding type.
     * @param prefixBucketName
     *            a short prefix to the bucket name.
     * @param primaryConfig
     *            data location primary config
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType dataShardingType,
            String prefixBucketName, ScmCephS3UserConfig primaryConfig)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(dataShardingType, "dataShardingType");
        checkValueNotNull(prefixBucketName, "prefixBucketName");
        checkValueNotNull(primaryConfig, "primaryConfig");
        this.shardingType = dataShardingType;
        this.primaryConfig = primaryConfig;
        setPrefixBucketName(prefixBucketName);
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
     * @param primaryConfig
     *            data location primary config
     * @param standbyConfig
     *            data location standby config
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType dataShardingType,
            String prefixBucketName, ScmCephS3UserConfig primaryConfig,
            ScmCephS3UserConfig standbyConfig) throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(dataShardingType, "dataShardingType");
        checkValueNotNull(prefixBucketName, "prefixBucketName");
        checkValueNotNull(primaryConfig, "primaryConfig");
        checkValueNotNull(standbyConfig, "standbyConfig");
        this.shardingType = dataShardingType;
        this.primaryConfig = primaryConfig;
        this.standbyConfig = standbyConfig;
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
     * @param bucketName
     *            bucket name(A certain bucket that already exists).
     * @param objectShardingType
     *            object sharding type.
     * @param primaryConfig
     *            data location primary config
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, String bucketName,
            ScmShardingType objectShardingType, ScmCephS3UserConfig primaryConfig)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(bucketName, "bucketName");
        checkValueNotNull(objectShardingType, "objectShardingType");
        checkValueNotNull(primaryConfig, "primaryConfig");
        this.objectShardingType = objectShardingType;
        this.bucketName = bucketName;
        this.primaryConfig = primaryConfig;
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
     * @param primaryConfig
     *            data location primary config
     * @param standbyConfig
     *            data location standby config
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, String bucketName,
            ScmShardingType objectShardingType, ScmCephS3UserConfig primaryConfig,
            ScmCephS3UserConfig standbyConfig) throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(bucketName, "bucketName");
        checkValueNotNull(objectShardingType, "objectShardingType");
        checkValueNotNull(primaryConfig, "primaryConfig");
        checkValueNotNull(standbyConfig, "standbyConfig");
        this.objectShardingType = objectShardingType;
        this.bucketName = bucketName;
        this.primaryConfig = primaryConfig;
        this.standbyConfig = standbyConfig;
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
     * @param dataShardingType
     *            data sharding type.
     * @param prefixBucketName
     *            a short prefix to the bucket name.
     * @param objectShardingType
     *            object sharding type.
     * @param primaryConfig
     *            data location primary config
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType dataShardingType,
            String prefixBucketName, ScmShardingType objectShardingType,
            ScmCephS3UserConfig primaryConfig) throws ScmInvalidArgumentException {
        this(siteName, dataShardingType, prefixBucketName, primaryConfig);
        checkValueNotNull(objectShardingType, "objectShardingType");
        this.objectShardingType = objectShardingType;
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
     * @param primaryConfig
     *            data location primary config
     * @param standbyConfig
     *            data location standby config
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public ScmCephS3DataLocation(String siteName, ScmShardingType dataShardingType,
                                 String prefixBucketName, ScmShardingType objectShardingType,
                                 ScmCephS3UserConfig primaryConfig, ScmCephS3UserConfig standbyConfig) throws ScmInvalidArgumentException {
        this(siteName, dataShardingType, prefixBucketName, primaryConfig, standbyConfig);
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
     * @param siteName
     *            site name.
     * @param primaryConfig
     *            data location primary config
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephS3DataLocation(String siteName, ScmCephS3UserConfig primaryConfig)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(primaryConfig, "primaryConfig");
        this.primaryConfig = primaryConfig;
    }

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param primaryConfig
     *            data location primary config
     * @param standbyConfig
     *            data location standby config
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephS3DataLocation(String siteName, ScmCephS3UserConfig primaryConfig,
            ScmCephS3UserConfig standbyConfig) throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(primaryConfig, "primaryConfig");
        checkValueNotNull(standbyConfig, "standbyConfig");
        this.primaryConfig = primaryConfig;
        this.standbyConfig = standbyConfig;
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
        // 新增字段，需要在 ScmCephS3DataLocation(BSONObject obj, boolean strict) 增加相应的字段进行校验
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

        BSONObject userInfo = (BSONObject) obj
                .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO);
        if (userInfo != null) {
            BSONObject primary = (BSONObject) userInfo
                    .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_PRIMARY);
            if (primary != null) {
                String primaryUser = (String) primary
                        .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER);
                String primaryPassword = (String) primary
                        .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD);
                setPrimaryConfig(new ScmCephS3UserConfig(primaryUser, primaryPassword));
            }

            BSONObject standby = (BSONObject) userInfo
                    .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_STANDBY);
            if (standby != null) {
                String standbyUser = (String) standby
                        .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER);
                String standbyPassword = (String) standby
                        .get(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD);
                setStandbyConfig(new ScmCephS3UserConfig(standbyUser, standbyPassword));
            }
        }
    }

    /**
     * Create ceph s3 data location with specified args.
     *
     * @param obj
     *            a bson containing information about ceph s3 location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmCephS3DataLocation(BSONObject obj, boolean strict)
            throws ScmInvalidArgumentException {
        this(obj);
        // strict 为 true 时，obj 中不能包含未定义的字段
        // 应与 ScmCephS3DataLocation(BSONObject obj) 中的解析的字段一致，
        // 根据业务需要，部分字段可缺省，但不可以有多余字段
        if (strict) {
            BSONObject objCopy = BsonUtils.deepCopyRecordBSON(obj);
            objCopy.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_OBJECT_SHARDING_TYPE);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_CONTAINER_PREFIX);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_BUCKET_NAME);

            BSONObject userInfo = (BSONObject) objCopy
                    .removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO);
            if (userInfo != null) {
                BSONObject primary = (BSONObject) userInfo
                        .removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_PRIMARY);
                if (primary != null) {
                    primary.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER);
                    primary.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD);
                    if (!primary.isEmpty()) {
                        throw new ScmInvalidArgumentException("contain invalid key:"
                                + FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO + "."
                                + FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_PRIMARY + "."
                                + primary.keySet());
                    }
                }

                BSONObject standby = (BSONObject) userInfo
                        .removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_STANDBY);
                if (standby != null) {
                    standby.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER);
                    standby.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD);
                    if (!standby.isEmpty()) {
                        throw new ScmInvalidArgumentException("contain invalid key:"
                                + FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO + "."
                                + FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_STANDBY + "."
                                + standby.keySet());
                    }
                }

                if (!userInfo.isEmpty()) {
                    throw new ScmInvalidArgumentException("contain invalid key:"
                            + FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO + "."
                            + userInfo.keySet());
                }
            }

            if (!objCopy.isEmpty()) {
                throw new ScmInvalidArgumentException("contain invalid key:" + objCopy.keySet());
            }
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
        if (primaryConfig != null || standbyConfig != null) {
            BSONObject info = new BasicBSONObject();
            if (primaryConfig != null) {
                BSONObject primaryInfo = new BasicBSONObject();
                primaryInfo.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER,
                        primaryConfig.getUser());
                primaryInfo.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD,
                        primaryConfig.getPasswordFile());
                info.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_PRIMARY, primaryInfo);
            }
            if (standbyConfig != null) {
                BSONObject standbyInfo = new BasicBSONObject();
                standbyInfo.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER,
                        standbyConfig.getUser());
                standbyInfo.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD,
                        standbyConfig.getPasswordFile());
                info.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_STANDBY, standbyInfo);
            }
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO, info);
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

    /**
     * Gets the primary config.
     *
     * @return primary config.
     */
    public ScmCephS3UserConfig getPrimaryConfig() {
        return primaryConfig;
    }

    /**
     * Sets the primary config.
     *
     * @param primaryConfig
     *            primary config.
     * @throws ScmInvalidArgumentException
     *             if bucket name is invalid.
     */
    public void setPrimaryConfig(ScmCephS3UserConfig primaryConfig)
            throws ScmInvalidArgumentException {
        checkValueNotNull(primaryConfig, "primaryConfig");
        this.primaryConfig = primaryConfig;
    }

    /**
     * Gets the standby config.
     *
     * @return standby config.
     */
    public ScmCephS3UserConfig getStandbyConfig() {
        return standbyConfig;
    }

    /**
     * Sets the standby config.
     *
     * @param standbyConfig
     *            bucket name.
     * @throws ScmInvalidArgumentException
     *             if bucket name is invalid.
     */
    public void setStandbyConfig(ScmCephS3UserConfig standbyConfig)
            throws ScmInvalidArgumentException {
        checkValueNotNull(standbyConfig, "standbyConfig");
        this.standbyConfig = standbyConfig;
    }
}
