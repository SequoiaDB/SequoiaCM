package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;

/**
 * Sequoiadb meta location.
 */
public class ScmSdbMetaLocation extends ScmMetaLocation {
    private String domainName;
    private ScmShardingType shardingType;
    private BSONObject csOptions;
    private BSONObject clOptions;

    /**
     * Create a sequoiadb meta location with specified args.
     *
     * @param siteName
     *            site name.
     * @param shardingType
     *            meta sharding type.
     * @param domainName
     *            domain name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbMetaLocation(String siteName, ScmShardingType shardingType, String domainName)
            throws ScmInvalidArgumentException {
        this(siteName, domainName);
        checkValueNotNull(shardingType, "sharidngType");
        if (shardingType == ScmShardingType.NONE) {
            throw new ScmInvalidArgumentException(
                    "unsupported shardingType:" + ScmShardingType.NONE.getName());
        }
        setShardingType(shardingType);
    }

    /**
     * Create sequoiadb meta location with specified arg.
     *
     * @param obj
     *            a bson containing information about sequoiadb location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbMetaLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        domainName = (String) obj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        if (domainName == null) {
            throw new ScmInvalidArgumentException("missing field:fieldName="
                    + FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN + ", obj=" + obj);
        }
        String sharding = (String) obj.get(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE);
        if (sharding != null) {
            ScmShardingType metaShardingType = ScmShardingType.getShardingType(sharding);
            if (metaShardingType == null) {
                throw new ScmInvalidArgumentException("unknown sharding type:" + obj);
            }
            setShardingType(metaShardingType);
        }

        BSONObject metaOptions = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_META_OPTIONS);
        if (metaOptions != null) {
            clOptions = (BSONObject) metaOptions.get(FieldName.FIELD_CLWORKSPACE_META_CL);
            csOptions = (BSONObject) metaOptions.get(FieldName.FIELD_CLWORKSPACE_META_CS);
        }
    }

    /**
     * Create a sequoiadb meta location with specified args.
     *
     * @param siteName
     *            site name.
     * @param domainName
     *            domain name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmSdbMetaLocation(String siteName, String domainName)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(domainName, "domainName");
        this.domainName = domainName;
    }

    /**
     * Gets the domain name.
     *
     * @return domain.
     */
    public String getDomainName() {
        return domainName;
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bson = super.getBSONObject();
        bson.put(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN, getDomainName());
        if (shardingType != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE, shardingType.getName());
        }

        BSONObject metaOptions = new BasicBSONObject();
        if (csOptions != null) {
            metaOptions.put(FieldName.FIELD_CLWORKSPACE_META_CS, csOptions);
        }
        if (clOptions != null) {
            metaOptions.put(FieldName.FIELD_CLWORKSPACE_META_CL, clOptions);
        }
        if (!metaOptions.isEmpty()) {
            bson.put(FieldName.FIELD_CLWORKSPACE_META_OPTIONS, metaOptions);
        }

        return bson;
    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.SEQUOIADB;
    }

    /**
     * Gets the meta sharding type.
     *
     * @return meta sharding type.
     */
    public ScmShardingType getShardingType() {
        return shardingType;
    }

    /**
     * Sets the meta sharding type.
     *
     * @param shardingType
     *            meta sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setShardingType(ScmShardingType shardingType) throws ScmInvalidArgumentException {
        checkValueNotNull(shardingType, "sharidngType");
        if (shardingType == ScmShardingType.NONE) {
            throw new ScmInvalidArgumentException(
                    "unsupported shardingType:" + ScmShardingType.NONE.getName());
        }
        this.shardingType = shardingType;
    }

    /**
     * Gets the collectionspace options.
     *
     * @return collectionspace options
     */
    public BSONObject getCsOptions() {
        return csOptions;
    }

    /**
     * Sets the collectionspace options.
     *
     * @param csOptions
     *            collection space options.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setCsOptions(BSONObject csOptions) throws ScmInvalidArgumentException {
        checkValueNotNull(csOptions, "csOptions");
        this.csOptions = csOptions;
    }

    /**
     * Gets collection options.
     *
     * @return collection options.
     */
    public BSONObject getClOptions() {
        return clOptions;
    }

    /**
     * Sets collection options.
     *
     * @param clOptions
     *            collection option.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setClOptions(BSONObject clOptions) throws ScmInvalidArgumentException {
        checkValueNotNull(clOptions, "clOptions");
        this.clOptions = clOptions;
    }

}
