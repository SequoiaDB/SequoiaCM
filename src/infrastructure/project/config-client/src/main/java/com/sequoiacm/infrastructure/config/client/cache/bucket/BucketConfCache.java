package com.sequoiacm.infrastructure.config.client.cache.bucket;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.infrastructure.common.Pair;
import com.sequoiacm.infrastructure.common.ScmJsonInputStreamCursor;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketNotifyOption;


public class BucketConfCache {
    private static final Logger logger = LoggerFactory.getLogger(BucketConfCache.class);

    private final ScmConfClient confClient;

    private final ConcurrentLruMap<String, BucketConfig> bucketCacheNameMap;
    private final ConcurrentLruMap<Long, BucketConfig> bucketCacheIdMap;

    public BucketConfCache(BucketConfCacheConfig config, ScmConfClient confClient) {
        this.confClient = confClient;
        bucketCacheNameMap = ConcurrentLruMapFactory.create(config.getCacheLimit());
        bucketCacheIdMap = ConcurrentLruMapFactory.create(config.getCacheLimit());
    }

    @PostConstruct
    public void postConstruct() throws ScmConfigException {
        confClient.subscribe(ScmBusinessTypeDefine.BUCKET, new NotifyCallback() {
            @Override
            public void processNotify(EventType type, String businessName,
                    NotifyOption notification) {
                onBucketNotify(type, businessName, notification);
            }

            @Override
            public int priority() {
                return NotifyCallback.HIGHEST_PRECEDENCE;
            }
        });
        confClient.subscribe(ScmBusinessTypeDefine.WORKSPACE, new NotifyCallback() {
            @Override
            public void processNotify(EventType type, String businessName,
                    NotifyOption notification) {
                if (type == EventType.DELTE) {
                    invalidateBucketCacheByWs(businessName);
                }
            }

            @Override
            public int priority() {
                return NotifyCallback.HIGHEST_PRECEDENCE;
            }
        });
    }

    public BucketConfig getBucket(String bucketName) throws ScmConfigException {
        BucketConfig b = bucketCacheNameMap.get(bucketName);
        if (b != null) {
            return b;
        }
        return refreshBucketCache(bucketName);
    }

    public BucketConfig getBucketById(long bucketId) throws ScmConfigException {
        BucketConfig b = bucketCacheIdMap.get(bucketId);
        if (b != null) {
            return b;
        }

        return refreshBucketCacheById(bucketId);
    }

    public void invalidateBucketCache(String bucket) {
        removeCacheByName(bucket);
    }

    public void invalidateBucketCacheByWs(String wsName) {
        List<BucketConfig> buckets = bucketCacheNameMap.getValuesCopy();
        for (BucketConfig bucket : buckets) {
            if (bucket.getWorkspace().equals(wsName)) {
                removeCacheByName(bucket.getName());
            }
        }
    }

    private synchronized void invalidateAllCache() {
        bucketCacheIdMap.clear();
        bucketCacheNameMap.clear();
    }

    public ScmObjectCursor<BucketConfig> listBucket(BSONObject matcher, BSONObject orderby,
            long skip, long limit) throws ScmServerException {
        BucketConfigFilter filter = new BucketConfigFilter(matcher, orderby, limit, skip);
        try {
            final ScmJsonInputStreamCursor<Config> cursor = confClient
                    .listConf(ScmBusinessTypeDefine.BUCKET, filter);
            return new ScmObjectCursor<BucketConfig>() {
                @Override
                public boolean hasNext() throws IOException {
                    return cursor.hasNext();
                }

                @Override
                public BucketConfig getNext() throws IOException {
                    return (BucketConfig) cursor.getNext();
                }

                @Override
                public void close() {
                    cursor.close();
                }
            };
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to list bucket: matcher=" + matcher + ", orderby=" + orderby + ", skip="
                            + skip + ", limit=" + limit,
                    e);
        }
    }

    private void onBucketNotify(EventType type, String businessName, NotifyOption notification) {
        BucketNotifyOption bucketNotifyOption = (BucketNotifyOption) notification;
        if (bucketNotifyOption != null) {
            if (type == EventType.DELTE || type == EventType.UPDATE) {
                invalidateBucketCache(bucketNotifyOption.getBucketName());
                return;
            }
            else if (type == EventType.CREATE) {
                // 忽略
                return;
            }
        }
        invalidateAllCache();
    }

    private BucketConfig refreshBucketCache(String bucket) throws ScmConfigException {
        BucketConfig bucketConf = (BucketConfig) confClient.getOneConf(ScmBusinessTypeDefine.BUCKET,
                new BucketConfigFilter(bucket));

        if (bucketConf != null) {
            putCache(bucketConf);
            return bucketConf;
        }
        removeCacheByName(bucket);
        return null;
    }

    private BucketConfig refreshBucketCacheById(long bucketId) throws ScmConfigException {
        BucketConfigFilter filter = new BucketConfigFilter(
                new BasicBSONObject(FieldName.Bucket.ID, bucketId), null, -1, 0);
        BucketConfig bucketConf = (BucketConfig) confClient.getOneConf(ScmBusinessTypeDefine.BUCKET,
                filter);
        if (bucketConf != null) {
            putCache(bucketConf);
            return bucketConf;
        }
        removeCacheById(bucketId);
        return null;
    }

    private synchronized void putCache(BucketConfig bucketConfig) {
        Pair<BucketConfig> pair = bucketCacheNameMap.putWithReturnPair(bucketConfig.getName(),
                bucketConfig);
        bucketCacheIdMap.put(bucketConfig.getId(), bucketConfig);
        if (pair.getOverflowValue() != null) {
            bucketCacheIdMap.remove(pair.getOverflowValue().getId());
        }
    }

    private synchronized void removeCacheByName(String name) {
        BucketConfig v = bucketCacheNameMap.remove(name);
        if (v != null) {
            bucketCacheIdMap.remove(v.getId());
        }
    }

    private synchronized void removeCacheById(long id) {
        BucketConfig v = bucketCacheIdMap.remove(id);
        if (v != null) {
            bucketCacheNameMap.remove(v.getName());
        }
    }
}
