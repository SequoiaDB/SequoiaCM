package com.sequoiacm.infrastructure.config.client.core.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.infrastructure.common.Pair;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilter;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigDefine;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketNotifyOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;


public class BucketConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(BucketConfSubscriber.class);
    private long heartbeatInterval;
    @Value("${spring.application.name}")
    private String myServiceName;
    private ScmConfClient confClient;

    private DefaultVersionFilter versionFilter;

    private final ConcurrentLruMap<String, BucketConfig> bucketCacheNameMap;
    private final ConcurrentLruMap<Long, BucketConfig> bucketCacheIdMap;

    public BucketConfSubscriber(BucketSubscriberConfig config, ScmConfClient confClient) throws ScmConfigException {
        this.confClient = confClient;

        // client 的版本心跳线程只查询一条全局版本，服务端任意bucket被更新都会修改这个版本号
        // 并且某个bucket发生更新时， 更新通知会同时携带全局版本和该bucket的自身版本
        // client 的版本心跳线程会从bucket通知中拿到全局版本，更新自己的版本号
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.BUCKET);
        this.versionFilter.addBussinessName(BucketConfigDefine.ALL_BUCKET_VERSION);
        bucketCacheNameMap = ConcurrentLruMapFactory.create(config.getCacheLimit());
        bucketCacheIdMap = ConcurrentLruMapFactory.create(config.getCacheLimit());
        this.heartbeatInterval = config.getHeartbeatInterval();
    }

    @PostConstruct
    public void postConstruct() throws ScmConfigException {
        confClient.subscribeWithAsyncRetry(this);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.BUCKET;
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

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive bucket notification: {}", notification);
        BucketNotifyOption bucketNotifyOption = (BucketNotifyOption) notification;
        if (notification.getEventType() == EventType.DELTE) {
            invalidateBucketCache(bucketNotifyOption.getBucketName());
            return;
        }
        else if (notification.getEventType() == EventType.CREATE) {
            // 忽略
            return;
        }
        // notification.getEventType() == EventType.UPDATE
        // 更新通知有两个途径触发
        // 1. 配置服务端，执行了 bucket 更新 （目前Bucket没有实现更新），将特定 bucket 的更新通知投递过来，此时通知上包含桶名
        // 2.配置客户端比对版本时，发现版本不一致，自行生成通知调用此函数通知业务（BucketInfoMgr），此时通知不包含同名（bucket全局只有一个版本），即希望做本地所有
        // bucket 缓存的做更新
        if (bucketNotifyOption.getBucketName() != null) {
            refreshBucketCache(bucketNotifyOption.getBucketName());
            return;
        }
        invalidateAllCache();
    }

    @Override
    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    @Override
    public long getHeartbeatIterval() {
        return heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        // client 轮询 bucket 全局版本时发现全局版本不一致，调用此函数将版本对象生成一个通知对象，
        // 我们这里生成一个 bucketName 是 null 的通知，processNotify 处理这个通知时会淘汰内存中的所有 bucket 缓存
        return new BucketNotifyOption(null, -1, EventType.UPDATE, version.getVersion());
    }

    private BucketConfig refreshBucketCache(String bucket) throws ScmConfigException {
        BucketConfig bucketConf = (BucketConfig) confClient.getOneConf(ScmConfigNameDefine.BUCKET,
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
        BucketConfig bucketConf = (BucketConfig) confClient.getOneConf(ScmConfigNameDefine.BUCKET,
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
