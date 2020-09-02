package com.sequoiadb.infrastructure.map.client.core;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.client.model.ScmEntry;

class ScmEntryImpl<K, V> implements ScmEntry<K, V> {
    private K key;
    private V value;
    private Class<?> valueType;

    public ScmEntryImpl(BSONObject bson, Class<?> valueType) {
        if (valueType == null) {
            throw new RuntimeException("value type is null");
        }
        this.valueType = valueType;
        if (null != bson) {
            this.key = (K) bson.get(CommonDefine.FieldName.KEY);
            Object valueObj = bson.get(CommonDefine.FieldName.VALUE);
            if (valueType.isPrimitive() || BSONObject.class.isAssignableFrom(valueType)) {
                this.value = (V) valueObj;
            }
            else {
                this.value = (V) JSONObject.parseObject(bson.toString(), valueType);
            }
        }
    }

    public ScmEntryImpl(K key, V value, Class<?> valueType) {
        if (valueType == null) {
            throw new RuntimeException("value type is null");
        }
        this.valueType = valueType;
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    public BSONObject toBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(CommonDefine.FieldName.KEY, key);
        if (valueType.isPrimitive() || BSONObject.class.isAssignableFrom(valueType)) {
            bson.put(CommonDefine.FieldName.VALUE, value);
        }
        else {
            bson.put(CommonDefine.FieldName.VALUE, JSON.parse(JSONObject.toJSONString(value)));
        }
        return bson;
    }

    @Override
    public V setValue(V value) {
        return value;
    }

}
