package com.sequoiacm.s3.context;

import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.exception.S3ServerException;

public class S3ListObjectContext {
    private static final Logger logger = LoggerFactory.getLogger(S3ListObjectContext.class);
    private final S3ListObjContextMgr mgr;
    private boolean isPersistence;
    private String token;
    private String prefix;
    private String startAfter;
    private String delimiter;
    private String lastMarker;
    private String bucketName;
    private long lastAccessTime;

    public S3ListObjectContext(String prefix, String startAfter, String delimiter,
            String bucketName, boolean isPersistence, S3ListObjContextMgr mgr) {
        this.prefix = prefix;
        this.startAfter = startAfter;
        this.delimiter = delimiter;
        this.bucketName = bucketName;
        this.token = UUID.randomUUID().toString();
        this.lastAccessTime = System.currentTimeMillis();
        this.isPersistence = isPersistence;
        this.lastMarker = startAfter;
        this.mgr = mgr;
    }

    public S3ListObjectContext(BSONObject record, boolean isPersistence, S3ListObjContextMgr mgr) {
        token = BsonUtils.getStringChecked(record, S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_TOKEN);
        prefix = BsonUtils.getString(record, S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_PREFIX);
        startAfter = BsonUtils.getString(record,
                S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_START_AFTER);
        delimiter = BsonUtils.getString(record, S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_DELIMITER);
        lastMarker = BsonUtils.getString(record,
                S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_MARKER);
        bucketName = BsonUtils.getStringChecked(record,
                S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_BUCKET_NAME);
        lastAccessTime = BsonUtils.getLongChecked(record,
                S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_ACCESS_TIME);
        this.mgr = mgr;
        this.isPersistence = isPersistence;
    }

    @Override
    public String toString() {
        return "S3ListObjectContext{" + "mgr=" + mgr + ", isPersistence=" + isPersistence
                + ", token='" + token + '\'' + ", prefix='" + prefix + '\'' + ", startAfter='"
                + startAfter + '\'' + ", delimiter='" + delimiter + '\'' + ", lastMarker='"
                + lastMarker + '\'' + ", bucketName='" + bucketName + '\'' + ", lastAccessTime="
                + lastAccessTime + '}';
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getStartAfter() {
        return startAfter;
    }

    public void setStartAfter(String startAfter) {
        this.startAfter = startAfter;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getLastMarker() {
        return lastMarker;
    }

    public void setLastMarker(String lastMarker) {
        this.lastMarker = lastMarker;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void save() throws S3ServerException {
        lastAccessTime = System.currentTimeMillis();
        mgr.save(this);
        isPersistence = true;
    }

    boolean isPersistence() {
        return isPersistence;
    }

    public void release() {
        try {
            if (isPersistence) {
                mgr.remove(token);
            }
        }
        catch (Exception e) {
            logger.warn("failed to release context:{}", token, e);
        }
    }

    BSONObject toBSON() {
        BSONObject ret = new BasicBSONObject();
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_TOKEN, token);
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_DELIMITER, delimiter);
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_BUCKET_NAME, bucketName);
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_MARKER, lastMarker);
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_ACCESS_TIME, lastAccessTime);
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_PREFIX, prefix);
        ret.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_START_AFTER, startAfter);
        return ret;
    }
}
