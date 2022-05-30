package com.sequoiacm.contentserver.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.infrastructure.common.Pair;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BucketInfoManager {
    private static final Logger logger = LoggerFactory.getLogger(BucketInfoManager.class);
    private final int cacheLimit;
    private final ConcurrentLruMap<String, ScmBucket> bucketCacheNameMap;
    private final ConcurrentHashMap<Long, ScmBucket> bucketCacheIdMap;

    @Autowired
    public BucketInfoManager(ScmBucketConfig config) {
        this.cacheLimit = config.getCacheLimit();
        bucketCacheNameMap = ConcurrentLruMapFactory.create(cacheLimit);
        bucketCacheIdMap = new ConcurrentHashMap<>(cacheLimit);
    }

    public void addBucketCache(String bucket) throws ScmServerException {
        refreshBucketCache(bucket);
    }

    public synchronized void invalidateAllCache() {
        bucketCacheIdMap.clear();
        bucketCacheNameMap.clear();
    }

    public void refreshBucketCache() throws ScmServerException {
        Set<String> bucketNames = bucketCacheNameMap.getKeySetCopy();

        List<String> nameBatch = new ArrayList<>();
        for (String name : bucketNames) {
            nameBatch.add(name);
            if (nameBatch.size() >= 200) {
                refreshBucketCache(nameBatch);
                nameBatch.clear();
            }
        }
        if (nameBatch.size() > 0) {
            refreshBucketCache(nameBatch);
        }

    }

    private synchronized void putCache(ScmBucket bucketInfo) {
        Pair<ScmBucket> pair = bucketCacheNameMap.putWithReturnPair(bucketInfo.getName(),
                bucketInfo);
        bucketCacheIdMap.put(bucketInfo.getId(), bucketInfo);
        if (pair.getOverflowValue() != null) {
            bucketCacheIdMap.remove(pair.getOverflowValue().getId());
        }
    }

    private synchronized void removeCacheByName(String name) {
        ScmBucket v = bucketCacheNameMap.remove(name);
        if (v != null) {
            bucketCacheIdMap.remove(v.getId());
        }
    }

    public ScmBucket getBucketById(long id) throws ScmServerException {
        ScmBucket b = bucketCacheIdMap.get(id);
        if (b != null) {
            return b;
        }

        b = ContenserverConfClient.getInstance().getBucketById(id);
        if (b != null) {
            putCache(b);
        }
        return b;
    }
    private void refreshBucketCache(BSONObject matcher, int limit) throws ScmServerException {
        try (ScmObjectCursor<ScmBucket> cursor = ContenserverConfClient.getInstance()
                .listBucket(matcher, null, 0, limit)) {
            while (cursor.hasNext()) {
                ScmBucket bucket = cursor.getNext();
                putCache(bucket);
            }
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.NETWORK_IO,
                    "failed to list bucket:matcher=" + matcher, e);
        }
    }

    private void refreshBucketCache(List<String> nameBatch) throws ScmServerException {
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Bucket.NAME, new BasicBSONObject("$in", nameBatch));
        refreshBucketCache(matcher, -1);
    }

    public void refreshBucketCache(String bucket) throws ScmServerException {
        ScmBucket bucketInfo = ContenserverConfClient.getInstance().getBucket(bucket);
        if (bucketInfo != null) {
            putCache(bucketInfo);
            return;
        }
        removeCacheByName(bucket);
    }

    public void invalidateBucketCache(String bucket) {
        removeCacheByName(bucket);
    }

    public void invalidateBucketCacheByWs(String wsName) {
        List<ScmBucket> buckets = bucketCacheNameMap.getValuesCopy();
        for (ScmBucket bucket : buckets) {
            if (bucket.getWorkspace().equals(wsName)) {
                removeCacheByName(bucket.getName());
            }
        }
    }

    public ScmBucket getBucket(String name) throws ScmServerException {
        ScmBucket b = bucketCacheNameMap.get(name);
        if (b != null) {
            return b;
        }

        b = ContenserverConfClient.getInstance().getBucket(name);
        if (b != null) {
            putCache(b);
        }
        return b;
    }

}
