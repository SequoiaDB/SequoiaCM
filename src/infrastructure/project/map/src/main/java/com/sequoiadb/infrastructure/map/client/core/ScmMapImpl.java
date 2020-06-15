package com.sequoiadb.infrastructure.map.client.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.alibaba.fastjson.JSONObject;
import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapRuntimeException;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.BsonConverter;
import com.sequoiadb.infrastructure.map.client.BsonReader;
import com.sequoiadb.infrastructure.map.client.ScmBsonCursor;
import com.sequoiadb.infrastructure.map.client.ScmCursor;
import com.sequoiadb.infrastructure.map.client.model.ScmMap;
import com.sequoiadb.infrastructure.map.client.service.IMapClientService;

class ScmMapImpl<K, V> implements ScmMap<K, V> {
    private String mapName;
    private Class<?> keyType;
    private Class<?> valueType;
    private IMapClientService clientService;

    public ScmMapImpl(IMapClientService mapClientService, BSONObject mapBson) {
        parseBson(mapBson);
        this.clientService = mapClientService;
    }

    @Override
    public Class<?> getkeyType() {
        return keyType;
    }

    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    @Override
    public String getName() {
        return mapName;
    }

    private void parseBson(BSONObject mapBson) {
        if (mapBson != null) {
            this.mapName = (String) mapBson.get(CommonDefine.FieldName.MAP_NAME);
            String keyTypeStr = (String) mapBson.get(CommonDefine.FieldName.MAP_KEY_TYPE);
            String valueTypeStr = (String) mapBson.get(CommonDefine.FieldName.MAP_VALUE_TYPE);
            try {
                this.keyType = Class.forName(keyTypeStr);
                this.valueType = Class.forName(valueTypeStr);
            }
            catch (ClassNotFoundException e) {
                new RuntimeException("map key or value type not found", e);
            }
        }
    }

    @Override
    public int size() {
        try {
            long count = clientService.count(mapName, null);
            if (count <= Integer.MAX_VALUE) {
                return (int) count;
            }
            throw new ScmMapRuntimeException(ScmMapError.NUMBER_CROSS_BOUNDER,
                    "size greater than Integer range, size=" + count);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException("get map size failed", e);
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            long count = clientService.count(mapName, null);
            return count == 0;
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            BSONObject filter = new BasicBSONObject(CommonDefine.FieldName.KEY, key);
            long count = clientService.count(mapName, filter);
            return count != 0;
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public boolean containKeySet(Collection<?> c) {
        try {
            BasicBSONList keyList = new BasicBSONList();
            for (Object key : c) {
                BSONObject keyBson = new BasicBSONObject(CommonDefine.FieldName.KEY, key);
                keyList.add(keyBson);

            }
            BSONObject filter = new BasicBSONObject(CommonDefine.Mather.OR, keyList);
            long count = clientService.count(mapName, filter);
            return c.size() == count;

        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public V get(Object key) {
        try {
            BSONObject filter = new BasicBSONObject(CommonDefine.FieldName.KEY, key);
            BSONObject valueBson = clientService.get(mapName, filter);
            return getValue(valueBson);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    private V getValue(BSONObject valueBson) {
        if (valueBson != null) {
            if (valueType.isPrimitive() || BSONObject.class.isAssignableFrom(valueType)) {
                return (V) valueBson.get(CommonDefine.FieldName.VALUE);
            }
            BSONObject bson = (BSONObject) valueBson.get(CommonDefine.FieldName.VALUE);
            return (V) JSONObject.parseObject(bson.toString(), valueType);
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        checkKV(key, value);
        try {
            BSONObject valueBson = clientService.put(mapName,
                    new ScmEntryImpl<K, V>(key, value, valueType));
            return getValue(valueBson);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    private void checkKV(K key, V value) {
        if ((key != null && !keyType.isAssignableFrom(key.getClass()))
                || (value != null && !valueType.isAssignableFrom(value.getClass()))) {
            throw new ScmMapRuntimeException(ScmMapError.PUT_CLASS_ERROR,
                    "put key or value type error: keyType=" + keyType + ", valueType=" + valueType);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        try {
            Set<? extends K> keySet = m.keySet();
            List<BSONObject> entryList = new ArrayList<>();
            for (K key : keySet) {
                V value = m.get(key);
                checkKV(key, value);
                entryList.add(new ScmEntryImpl<K, V>(key, value, valueType).toBson());
            }
            clientService.putAll(mapName, entryList);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public V remove(Object key) {
        try {
            BSONObject filter = new BasicBSONObject(CommonDefine.FieldName.KEY, key);
            BSONObject valueBson = clientService.remove(mapName, filter);
            return getValue(valueBson);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public boolean removeKeySet(Collection<?> c) {
        try {
            BasicBSONList keyList = new BasicBSONList();
            for (Object key : c) {
                BSONObject keyBson = new BasicBSONObject(CommonDefine.FieldName.KEY, key);
                keyList.add(keyBson);
            }
            BSONObject filter = new BasicBSONObject(CommonDefine.Mather.OR, keyList);
            return clientService.removeAll(mapName, filter);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public boolean retainKeySet(Collection<?> c) {
        try {
            BSONObject ninSet = new BasicBSONObject(CommonDefine.Mather.NIN, c);
            BSONObject filter = new BasicBSONObject(CommonDefine.FieldName.KEY, ninSet);
            return clientService.removeAll(mapName, filter);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public void clear() {
        try {
            clientService.removeAll(mapName, null);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public ScmCursor<K> listKey(BSONObject filter, BSONObject orderby, long skip, long limit) {
        try {
            BsonReader reader = clientService.listKey(mapName, filter, orderby, skip, limit);
            return new ScmBsonCursor<K>(reader, new BsonConverter<K>() {
                @Override
                public K convert(BSONObject obj) throws ScmMapServerException {
                    return (K) obj.get(CommonDefine.FieldName.KEY);
                }
            });
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public ScmCursor<Entry<K, V>> listEntry(BSONObject filter, BSONObject orderby, long skip,
            long limit) {
        try {
            BsonReader reader = clientService.listEntry(mapName, filter, orderby, skip, limit);
            return new ScmBsonCursor<Entry<K, V>>(reader, new BsonConverter<Entry<K, V>>() {
                @Override
                public Entry<K, V> convert(BSONObject obj) throws ScmMapServerException {
                    return new ScmEntryImpl<K, V>(obj, valueType);
                }
            });

        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
    }

    @Override
    public Set<K> keySet() {
        return new ScmSetImpl<K>(this, true);

    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ScmSetImpl<Entry<K, V>>(this, false);

    }

    @Override
    public String toString() {
        return "ScmMapImpl [mapName=" + mapName + ", keyType=" + keyType + ", valueType="
                + valueType + "]";
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    // @Override
    // public boolean containEntry(Object o) {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public boolean containEntrySet(Collection<?> c) {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public boolean removeEntrySet(Collection<?> c) {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public boolean retainEntrySet(Collection<?> c) {
    // throw new UnsupportedOperationException();
    // }
}