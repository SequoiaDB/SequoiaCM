package com.sequoiacm.contentserver.quota;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class QuotaConfigDetail {

    private String type;
    private String name;
    private long maxSize = -1;
    private long maxObjects = -1;
    private int quotaRoundNumber;
    private long usedSize;
    private long usedObjects;
    private boolean enable;
    private BSONObject extraInfo;

    public QuotaConfigDetail(BSONObject bsonObject) {
        this.type = BsonUtils.getStringChecked(bsonObject, FieldName.Quota.TYPE);
        this.name = BsonUtils.getStringChecked(bsonObject, FieldName.Quota.NAME);
        this.maxSize = BsonUtils.getNumberChecked(bsonObject, FieldName.Quota.MAX_SIZE).longValue();
        this.maxObjects = BsonUtils.getNumberChecked(bsonObject, FieldName.Quota.MAX_OBJECTS)
                .longValue();
        this.usedSize = BsonUtils.getNumberChecked(bsonObject, FieldName.Quota.USED_SIZE)
                .longValue();
        this.usedObjects = BsonUtils.getNumberChecked(bsonObject, FieldName.Quota.USED_OBJECTS)
                .longValue();
        this.enable = BsonUtils.getBooleanChecked(bsonObject, FieldName.Quota.ENABLE);
        this.extraInfo = BsonUtils.getBSON(bsonObject, FieldName.Quota.EXTRA_INFO);
        this.quotaRoundNumber = BsonUtils
                .getNumberChecked(bsonObject, FieldName.Quota.QUOTA_ROUND_NUMBER).intValue();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public long getMaxObjects() {
        return maxObjects;
    }

    public void setMaxObjects(long maxObjects) {
        this.maxObjects = maxObjects;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public long getUsedObjects() {
        return usedObjects;
    }

    public void setUsedObjects(long usedObjects) {
        this.usedObjects = usedObjects;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }

    public int getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    public void setQuotaRoundNumber(int quotaRoundNumber) {
        this.quotaRoundNumber = quotaRoundNumber;
    }

    @Override
    public String toString() {
        return "QuotaConfigDetail{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", maxSize=" + maxSize + ", maxObjects=" + maxObjects + ", quotaRoundNumber="
                + quotaRoundNumber + ", usedSize=" + usedSize + ", usedObjects=" + usedObjects
                + ", enable=" + enable + ", extraInfo=" + extraInfo + '}';
    }
}
