package com.sequoiacm.common.mapping;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;

public class ScmWorkspaceObj {
    private int id;
    private String name;
    private BSONObject metaLocation;
    private List<BSONObject> dataLocation = new ArrayList<BSONObject>();
    private BSONObject dataOption;
    private BSONObject dataShardingType;
    private String metaShardingType;
    private String descriptions;
    private long createTime;
    private long updateTime;
    private String createUser;
    private String updateUser;

    public ScmWorkspaceObj(BSONObject obj) throws ScmMappingException {
        try {
            Object tmp = null;

            id = (Integer) getValueCheckNotNull(obj, FieldName.FIELD_CLWORKSPACE_ID);

            name = (String) getValueCheckNotNull(obj, FieldName.FIELD_CLWORKSPACE_NAME);

            descriptions = getOrElse(obj, FieldName.FIELD_CLWORKSPACE_DESCRIPTION, "");

            createTime = getNumOrElse(obj, FieldName.FIELD_CLWORKSPACE_CREATETIME, 0L).longValue();

            updateTime = getNumOrElse(obj, FieldName.FIELD_CLWORKSPACE_UPDATETIME, 0L).longValue();

            createUser = getOrElse(obj, FieldName.FIELD_CLWORKSPACE_CREATEUSER, "");

            updateUser = getOrElse(obj, FieldName.FIELD_CLWORKSPACE_UPDATEUSER, "");

            metaLocation = (BSONObject) getValueCheckNotNull(obj,
                    FieldName.FIELD_CLWORKSPACE_META_LOCATION);

            metaShardingType = (String) obj.get(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE);

            tmp = obj.get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
            if (tmp != null) {
                for (Object location : (BasicBSONList) tmp) {
                    dataLocation.add((BSONObject) location);
                }
            }

            dataOption = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS);
            dataShardingType = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        }
        catch (Exception e) {
            throw new ScmMappingException("parse workspaceMap info failed:record=" + obj.toString(),
                    e);
        }
    }

    private Object getValueCheckNotNull(BSONObject obj, String key) throws ScmMappingException {
        Object value = obj.get(key);
        if (value == null) {
            throw new ScmMappingException("field is not exist:fieldName=" + key);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private <F> F getOrElse(BSONObject obj, String key, F defaultValue) {
        Object v = obj.get(key);
        if (v == null) {
            return defaultValue;
        }
        return (F) v;
    }

    private Number getNumOrElse(BSONObject obj, String key, Number defaultValue) {
        return getOrElse(obj, key, defaultValue);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BSONObject getMetaLocation() {
        return metaLocation;
    }

    public List<BSONObject> getDataLocation() {
        return dataLocation;
    }

    public BSONObject getDataOption() {
        return dataOption;
    }

    public BSONObject getDataShardingType() {
        return dataShardingType;
    }

    public String getMetaShardingType() {
        return metaShardingType;
    }

    public String getDescriptions() {
        return descriptions;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

}
