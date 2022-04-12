package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigDefine;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketNotifyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

public class BucketConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(BucketConfSubscriber.class);
    private final BucketInfoManager bucketInfoMgr;
    private long heartbeatInterval;
    private String myServiceName;
    private DefaultVersionFilter versionFilter;

    public BucketConfSubscriber(BucketInfoManager bucketInfoManager, String myServiceName,
            long heartbeatInterval) {
        this.myServiceName = myServiceName;
        this.heartbeatInterval = heartbeatInterval;
        this.bucketInfoMgr = bucketInfoManager;

        // client 的版本心跳线程只查询一条全局版本，服务端任意bucket被更新都会修改这个版本号
        // 并且某个bucket发生更新时， 更新通知会同时携带全局版本和该bucket的自身版本
        // client 的版本心跳线程会从bucket通知中拿到全局版本，更新自己的版本号
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.BUCKET);
        this.versionFilter.addBussinessName(BucketConfigDefine.ALL_BUCKET_VERSION);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.BUCKET;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        BucketNotifyOption bucketNotifyOption = (BucketNotifyOption) notification;
        if (notification.getEventType() == EventType.DELTE) {
            bucketInfoMgr.invalidateBucketCache(bucketNotifyOption.getBucketName());
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
            bucketInfoMgr.addBucketCache(bucketNotifyOption.getBucketName());
            return;
        }
        bucketInfoMgr.invalidateAllCache();

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
}
