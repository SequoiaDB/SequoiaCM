package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
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
        // 新增字段，需要在 ScmHbaseDataLocation(BSONObject obj, boolean strict) 增加相应的字段进行校验
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
     * Create a habse data location with specified arg.
     *
     * @param obj
     *            a bson containing information about hbase location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmHbaseDataLocation(BSONObject obj, boolean strict) throws ScmInvalidArgumentException {
        this(obj);
        // strict 为 true 时，obj 中不能包含未定义的字段
        // 应与 ScmHbaseDataLocation(BSONObject obj) 中的解析的字段一致，
        // 根据业务需要，部分字段可缺省，但不可以有多余字段
        if (strict) {
            BSONObject objCopy = BsonUtils.deepCopyRecordBSON(obj);
            objCopy.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            objCopy.removeField(FieldName.FIELD_CLWORKSPACE_HABSE_NAME_SPACE);

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
