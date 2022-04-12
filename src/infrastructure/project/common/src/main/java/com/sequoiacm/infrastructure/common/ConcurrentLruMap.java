package com.sequoiacm.infrastructure.common;

import java.util.List;
import java.util.Set;

public interface ConcurrentLruMap<K, V> {
    public V put(K key, V value);

    // 返回一对值，一个表示覆盖的值（如果发生覆盖），一个表示被淘汰的值（如果发生了淘汰）
    public Pair<V> putWithReturnPair(K key, V value);

    public V remove(K key);

    public Set<K> getKeySetCopy();

    public V get(K key);

    public void clear();

    public List<V> getValuesCopy();

}
