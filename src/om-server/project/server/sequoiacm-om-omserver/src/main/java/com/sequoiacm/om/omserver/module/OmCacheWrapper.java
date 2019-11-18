package com.sequoiacm.om.omserver.module;

public class OmCacheWrapper<T> {
    private long createTime;
    private T cache;

    public OmCacheWrapper(T cache) {
        this.createTime = System.currentTimeMillis();
        this.cache = cache;
    }

    public T getCache() {
        return cache;
    }

    public boolean isExpire(long cacheMaxTTL) {
        if (System.currentTimeMillis() - createTime > cacheMaxTTL) {
            return true;
        }
        return false;
    }
}
