package com.sequoiacm.infrastructure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConcurrentLruMapFactory {
    public static <K, V> ConcurrentLruMap<K, V> create(int size) {
        if (size > 0) {
            return new ConcurrentLruMapImpl<K, V>(size);
        }
        return new EmptyConcurrentLruMapImpl<K, V>();
    }

    public static void main(String[] args) {
        ConcurrentLruMap<String, String> map = create(3);
        map.put("1","1");
        map.put("2","2");
        map.put("3","3");
        Pair<String> pair = map.putWithReturnPair("1", "1");
System.out.println(pair);
    pair = map.putWithReturnPair("4", "4");
        System.out.println(pair);

    }
}


class EmptyConcurrentLruMapImpl<K, V> implements ConcurrentLruMap<K, V> {

    @Override
    public V put(K key, V value) {
        return null;
    }

    @Override
    public Pair<V> putWithReturnPair(K key, V value) {
        return new Pair<V>(null, null);
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

    @Override
    public List<V> getValuesCopy() {
        return Collections.EMPTY_LIST;
    }

}

class ConcurrentLruMapImpl<K, V> implements ConcurrentLruMap<K, V> {
    private LruMap<K, V> innerMap;

    public ConcurrentLruMapImpl(int size) {
        this.innerMap = new LruMap<K, V>(size);
    }

    @Override
    public synchronized V put(K key, V value) {
        return innerMap.put(key, value);
    }

    @Override
    public synchronized Pair<V> putWithReturnPair(K key, V value) {
        V oldValue = innerMap.put(key, value);
        V overflowValue = innerMap.getLastOverflowValue();
        return new Pair<V>(oldValue, overflowValue);
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
    public synchronized List<V> getValuesCopy() {
        return new ArrayList<V>(innerMap.values());
    }

    @Override
    public synchronized String toString() {
        return innerMap.toString();
    }
}

