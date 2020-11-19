package com.sequoiacm.infrastructure.common;

import java.util.Set;

public interface ConcurrentLruMap<K, V> {
    public V put(K key, V value);

    public V remove(K key);

    public Set<K> getKeySetCopy();

    public V get(K key);

    public void clear();

}
