package com.sequoiadb.infrastructure.map.client.model;

import java.util.Map.Entry;

import org.bson.BSONObject;

public interface ScmEntry<K, V> extends Entry<K, V> {
    public BSONObject toBson();
}
