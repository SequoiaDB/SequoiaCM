package com.sequoiacm.infrastructure.security.auth;

import org.apache.commons.collections.map.LRUMap;

public class ScmConcurrentLRUMap<K, V> {
    private LRUMap lruMap;

    public ScmConcurrentLRUMap(int maxCahcheSize) {
        lruMap = new LRUMap(maxCahcheSize);
    }

    @SuppressWarnings("unchecked")
    public synchronized V get(K key) {
        return (V) lruMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public synchronized V put(K key, V value) {
        return (V) lruMap.put(key, value);
    }

    public synchronized void clear() {
        lruMap.clear();
    }

}
