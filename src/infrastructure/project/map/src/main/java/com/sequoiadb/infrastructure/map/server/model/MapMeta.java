package com.sequoiadb.infrastructure.map.server.model;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiadb.infrastructure.map.CommonDefine;

public class MapMeta {
    private static final String MAP_CL_PREFIX = "MAP_";
    private String name;
    private String groupName;
    private String keyType;
    private String valueType;
    private String clName;
    private long lastTime;

    public MapMeta(BSONObject bson) {
        this.groupName = (String) bson.get(CommonDefine.FieldName.MAP_GROUP_NAME);
        this.name = (String) bson.get(CommonDefine.FieldName.MAP_NAME);
        this.keyType = (String) bson.get(CommonDefine.FieldName.MAP_KEY_TYPE);
        this.valueType = (String) bson.get(CommonDefine.FieldName.MAP_VALUE_TYPE);
        this.clName = (String) bson.get(CommonDefine.FieldName.MAP_CL_NAME);
    }

    public MapMeta(String groupName, String name, String keyType, String valueType) {
        this.groupName = groupName;
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
        this.clName = generaterClName(name);
    }

    private String generaterClName(String name) {
        return MAP_CL_PREFIX + name + "_" + ScmIdGenerator.MapId.get();
    }

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getValueType() {
        return valueType;
    }

    public String getClName() {
        return clName;
    }

    public BSONObject toBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(CommonDefine.FieldName.MAP_GROUP_NAME, groupName);
        bson.put(CommonDefine.FieldName.MAP_NAME, name);
        bson.put(CommonDefine.FieldName.MAP_KEY_TYPE, keyType);
        bson.put(CommonDefine.FieldName.MAP_VALUE_TYPE, valueType);
        bson.put(CommonDefine.FieldName.MAP_CL_NAME, clName);
        return bson;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void updateLastTime() {
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "MapMeta [name=" + name + ", groupName=" + groupName + ", keyType=" + keyType
                + ", valueType=" + valueType + "]";
    }

}
