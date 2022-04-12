package com.sequoiacm.infrastructure.config.core.msg.bucket;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class BucketNotifyOption implements NotifyOption {
    private String bucketName;
    private Integer version;
    private EventType eventType;
    private int globalVersion;

    public BucketNotifyOption(String bucketName, Integer version, EventType type,
            int globalVersion) {
        this.bucketName = bucketName;
        this.version = version;
        this.eventType = type;
        this.globalVersion = globalVersion;
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public DefaultVersion getVersion() {
        // conf client 版本心跳线程，通过这个接口获取通知中携带的版本号
        if (eventType == EventType.DELTE || eventType == EventType.CREATE) {
            return null;
        }
        // 只有 更新操作 需要通知 版本心跳线程 修改版本，目前Bucket的心跳线程只维护了一个 global version，所以这里把 global
        // version 返回心跳线程
        return new DefaultVersion(ScmConfigNameDefine.BUCKET, BucketConfigDefine.ALL_BUCKET_VERSION,
                globalVersion);
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Bucket.NAME, bucketName);
        obj.put(ScmRestArgDefine.BUCKET_CONF_VERSION, version);
        obj.put(ScmRestArgDefine.BUCKET_CONF_GLOBAL_VERSION, globalVersion);
        return obj;
    }

    @Override
    public String toString() {
        return "BucketNotifyOption{" + "bucketName='" + bucketName + '\'' + ", version=" + version
                + ", eventType=" + eventType + '}';
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }
}
