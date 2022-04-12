package com.sequoiacm.contentserver.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScmLRUMap extends LinkedHashMap<String, String> {
    private final int realMaxSize;

    // default: realMaxSize=10000, usage 2.4M
    // min: recordCount=100, usage 25.3KB
    public ScmLRUMap(int maxSize) {
        super(2 * maxSize, 0.75f, true);
        this.realMaxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        return size() > realMaxSize;
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    public synchronized String get(String key) {
        return super.get(key);
    }

    @Override
    public synchronized String put(String key, String value) {
        return super.put(key, value);
    }

    public synchronized String remove(String key) {
        return super.remove(key);
    }
}
