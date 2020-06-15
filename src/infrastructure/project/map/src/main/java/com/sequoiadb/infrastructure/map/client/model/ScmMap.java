package com.sequoiadb.infrastructure.map.client.model;

import java.util.Collection;
import java.util.Map;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.client.ScmCursor;

public interface ScmMap<K, V> extends Map<K, V> {
    boolean containKeySet(Collection<?> c);

    boolean removeKeySet(Collection<?> c);

    boolean retainKeySet(Collection<?> c);

    Class<?> getkeyType();

    Class<?> getValueType();

    String getName();

    ScmCursor<K> listKey(BSONObject filter, BSONObject orderby, long skip, long limit);

    ScmCursor<Entry<K, V>> listEntry(BSONObject filter, BSONObject orderby, long skip, long limit);

    // boolean containEntry(Object o);
    //
    // boolean containEntrySet(Collection<?> c);
    //
    // boolean removeEntrySet(Collection<?> c);
    //
    // boolean retainEntrySet(Collection<?> c);
}