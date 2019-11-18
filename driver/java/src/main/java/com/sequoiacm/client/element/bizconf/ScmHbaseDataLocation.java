package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;

/**
 * Hbase data location.
 */
public class ScmHbaseDataLocation extends ScmDataLocation {
    private ScmShardingType shardingType;
    private String namespace;

    /**
     * Create a hbase data location with specified args.
     *
     * @param siteName
     *            site name.
     * @param shardingType
     *            data sharding type.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHbaseDataLocation(String siteName, ScmShardingType shardingType)
            throws ScmInvalidArgumentException {
        super(siteName);
        checkValueNotNull(shardingType, "shardingType");
        this.shardingType = shardingType;
    }

    /**
     * Create a hbase data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHbaseDataLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Create a habse data location with specified arg.
     *
     * @param obj
     *            a bson containing information about hbase location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHbaseDataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
        String shardingStr = (String) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (shardingStr != null) {
            ScmShardingType sharding = ScmShardingType.getShardingType(shardingStr);
            if (sharding == null) {
                throw new ScmInvalidArgumentException("unknown sharding type:" + obj);
            }
            setShardingType(sharding);
        }

        String namespace = (String) obj.get(FieldName.FIELD_CLWORKSPACE_HABSE_NAME_SPACE);
        if (namespace != null) {
            setNamespace(namespace);
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

    /**
     * Returns hbase namespace.
     *
     * @return namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets a hbase namespace for store scm file data.
     *
     * @param namespace
     *            namespace.
     * @throws ScmInvalidArgumentException
     *             if namespace is invalid.
     */
    public void setNamespace(String namespace) throws ScmInvalidArgumentException {
        if (Strings.isEmpty(namespace)) {
            throw new ScmInvalidArgumentException("namespace is empty");
        }
        this.namespace = namespace;
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bson = super.getBSONObject();
        if (shardingType != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE, shardingType.getName());
        }
        if (namespace != null) {
            bson.put(FieldName.FIELD_CLWORKSPACE_HABSE_NAME_SPACE, namespace);
        }
        return bson;

    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.HBASE;
    }

}
