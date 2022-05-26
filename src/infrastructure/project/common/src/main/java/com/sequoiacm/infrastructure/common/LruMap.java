package com.sequoiacm.infrastructure.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 4721231572273075883L;
    private int maxSize;
    private volatile V lastOverflowValue;

    public LruMap(int size) {
        super(size, 0.75f, true);
        this.maxSize = size;

    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() > maxSize) {
            lastOverflowValue = eldest.getValue();
            return true;
        }
        return false;
    }

    public V getLastOverflowValue() {
        return lastOverflowValue;
    }
}