package com.sequoiacm.infrastructure.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ConcurrentLruMapFactory {
    public static <K, V> ConcurrentLruMap<K, V> create(int size) {
        if (size > 0) {
            return new ConcurrentLruMapImpl<K, V>(size);
        }
        return new EmptyConcurrentLruMapImpl<K, V>();
    }
}

class EmptyConcurrentLruMapImpl<K, V> implements ConcurrentLruMap<K, V> {

    @Override
    public V put(K key, V value) {
        return null;
    }

    @Override
    public V remove(K key) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<K> getKeySetCopy() {
        return Collections.EMPTY_SET;
    }

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public void clear() {
    }

}

class ConcurrentLruMapImpl<K, V> implements ConcurrentLruMap<K, V> {
    private Map<K, V> innerMap;

    public ConcurrentLruMapImpl(int size) {
        this.innerMap = new LruMap<K, V>(size);
    }

    @Override
    public synchronized V put(K key, V value) {
        return innerMap.put(key, value);
    }

    @Override
    public synchronized V remove(K key) {
        return innerMap.remove(key);
    }

    @Override
    public synchronized Set<K> getKeySetCopy() {
        return new HashSet<K>(innerMap.keySet());
    }

    @Override
    public synchronized V get(K key) {
        return innerMap.get(key);
    }

    @Override
    public synchronized void clear() {
        innerMap.clear();
    }

    @Override
    public synchronized String toString() {
        return innerMap.toString();
    }
}

class LruMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 4721231572273075883L;
    private int maxSize;

    public LruMap(int size) {
        super(size, 0.75f, true);
        this.maxSize = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() > maxSize) {
            return true;
        }
        return false;
    }
}
