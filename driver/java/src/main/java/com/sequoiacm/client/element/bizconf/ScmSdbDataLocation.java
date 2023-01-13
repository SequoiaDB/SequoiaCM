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
 * Sequoiadb data location.
 */
public class ScmSdbDataLocation extends ScmDataLocation {

    private String domainName;
    private ScmShardingType clShardingType;
    private ScmShardingType csShardingType;
    private BSONObject csOptions;
    private BSONObject clOptions;

    /**
     * Create a sequoiadb data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param domainName
     *            domain.
     * @param csShardingType
     *            collectionspace sharding type.
     * @param clShardingType
     *            collection sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbDataLocation(String siteName, String domainName, ScmShardingType csShardingType,
            ScmShardingType clShardingType) throws ScmInvalidArgumentException {
        this(siteName, domainName);
        checkValueNotNull(csShardingType, "csShardingType");
        checkValueNotNull(clShardingType, "clShardingType");
        if (clShardingType == ScmShardingType.NONE) {
            throw new ScmInvalidArgumentException(
                    "cl sharding type unsupport " + ScmShardingType.NONE.getName());
        }
        this.csShardingType = csShardingType;
        this.clShardingType = clShardingType;
    }

    /**
     * Create a sequoiadb data location with specified arg.
     *
     * @param obj
     *            a bson containing information about sequoiadb location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbDataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        // 新增字段，需要在 ScmSdbDataLocation(BSONObject obj, boolean strict) 增加相应的字段进行校验
        domainName = (String) obj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        BSONObject sharding = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (sharding != null) {
            String clShardingStr = (String) sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (clShardingStr != null) {
                ScmShardingType clSharding = ScmShardingType.getShardingType(clShardingStr);
                if (clSharding == null) {
                    throw new ScmInvalidArgumentException("unknown cl sharding type:" + obj);
                }
                setClShardingType(clSharding);
            }
            String csShardingStr = (String) sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (csShardingStr != null) {
                ScmShardingType csSharding = ScmShardingType.getShardingType(csShardingStr);
                if (csSharding == null) {
                    throw new ScmInvalidArgumentException("unknown cs sharding type:" + obj);
                }
                setCsShardingType(csSharding);
            }
        }

        BSONObject dataOptions = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS);
        if (dataOptions != null) {
            clOptions = (BSONObject) dataOptions.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            csOptions = (BSONObject) dataOptions.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
        }
    }

    /**
     * Create a sequoiadb data location with specified arg.
     *
     * @param obj
     *            a bson containing information about sequoiadb location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbDataLocation(BSONObject obj, boolean strict) throws ScmInvalidArgumentException {
        this(obj);
        // strict 为 true 时，obj 中不能包含未定义的字段
        // 应与 ScmSdbDataLocation(BSONObject obj) 中的解析的字段一致，
        // 根据业务需要，部分字段可缺省，不可以有多余字段
        if (strict) {
            BSONObject objCopy = BsonUtils.deepCopyRecordBSON(obj);
            objCopy.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
            BSONObject sharding = (BSONObject) objCopy
                    .removeField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            if (sharding != null) {
                sharding.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CL);
                sharding.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CS);
                if (!sharding.isEmpty()) {
                    throw new ScmInvalidArgumentException(
                            "contain invalid key:" + FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE
                                    + "." + sharding.keySet());
                }
            }

            BSONObject dataOptions = (BSONObject) objCopy
                    .removeField(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS);
            if (dataOptions != null) {
                dataOptions.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CL);
                dataOptions.removeField(FieldName.FIELD_CLWORKSPACE_DATA_CS);
                if (!dataOptions.isEmpty()) {
                    throw new ScmInvalidArgumentException(
                            "contain invalid key:" + dataOptions.keySet());
                }
            }

            if (!objCopy.isEmpty()) {
                throw new ScmInvalidArgumentException("contain invalid key:" + objCopy.keySet());
            }
        }
    }

    /**
     * Create a sequoiadb data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param domainName
     *            domain name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbDataLocation(String siteName, String domainName)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(domainName, "domainName");
        this.domainName = domainName;
    }

    /**
     * Create a sequoiadb data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbDataLocation(String siteName)
            throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Gets the domain name.
     *
     * @return domain name.
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Sets the domain name.
     *
     * @param domainName
     *            domain name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setDomainName(String domainName) throws ScmInvalidArgumentException {
        checkValueNotNull(domainName, "domainName");
        this.domainName = domainName;
    }

    /**
     * Gets the collection sharding type.
     *
     * @return collection shrading type.
     */
    public ScmShardingType getClShardingType() {
        return clShardingType;
    }

    /**
     * Sets the collection sharding type.
     *
     * @param clShardingType
     *            collection sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setClShardingType(ScmShardingType clShardingType)
            throws ScmInvalidArgumentException {
        checkValueNotNull(clShardingType, "clShardingType");
        if (clShardingType == ScmShardingType.NONE) {
            throw new ScmInvalidArgumentException(
                    "cl sharding type unsupport " + ScmShardingType.NONE.getName());
        }
        this.clShardingType = clShardingType;
    }

    /**
     * Gets the collectionspace sharding type.
     *
     * @return collectionspace sharding type.
     */
    public ScmShardingType getCsShardingType() {
        return csShardingType;
    }

    /**
     * Sets the collectionspace sharding type.
     *
     * @param csShardingType
     *            collectionspace sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setCsShardingType(ScmShardingType csShardingType)
            throws ScmInvalidArgumentException {
        checkValueNotNull(csShardingType, "csShardingType");
        this.csShardingType = csShardingType;
    }

    /**
     * Sets the collectionspace option.
     *
     * @param csOptions
     *            option.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setCsOptions(BSONObject csOptions) throws ScmInvalidArgumentException {
        checkValueNotNull(csOptions, "csOptions");
        this.csOptions = csOptions;
    }

    /**
     * Gets the collection space create option.
     *
     * @return option.
     */
    public BSONObject getCsOptions() {
        return csOptions;
    }

    /**
     * Sets the collection option.
     *
     * @param clOptions
     *            option.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setClOptions(BSONObject clOptions) throws ScmInvalidArgumentException {
        checkValueNotNull(clOptions, "clOptions");
        this.clOptions = clOptions;
    }

    /**
     * Gets the collection option.
     *
     * @return option.
     */
    public BSONObject getClOptions() {
        return clOptions;
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bson = super.getBSONObject();
        if (domainName != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN, domainName);
        }
        BSONObject shardingType = new BasicBSONObject();
        if (csShardingType != null) {
            shardingType.put(FieldName.FIELD_CLWORKSPACE_DATA_CS, getCsShardingType().getName());
        }
        if (clShardingType != null) {
            shardingType.put(FieldName.FIELD_CLWORKSPACE_DATA_CL, getClShardingType().getName());
        }
        if (!shardingType.isEmpty()) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE, shardingType);
        }
        BSONObject dataOptions = new BasicBSONObject();
        if (csOptions != null) {
            dataOptions.put(FieldName.FIELD_CLWORKSPACE_DATA_CS, csOptions);
        }
        if (clOptions != null) {
            dataOptions.put(FieldName.FIELD_CLWORKSPACE_DATA_CL, clOptions);
        }
        if (!dataOptions.isEmpty()) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS, dataOptions);
        }

        return bson;
    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.SEQUOIADB;
    }

}
