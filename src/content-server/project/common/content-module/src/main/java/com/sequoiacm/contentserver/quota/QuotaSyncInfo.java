package com.sequoiacm.contentserver.quota;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

public class QuotaSyncInfo {

    private String type;
    private String name;
    private long statisticsSize;
    private long expireTime;
    private long statisticsObjects;
    private int syncRoundNumber;
    private int quotaRoundNumber;
    private boolean isFirstSync;
    private String status;
    private Long agreementTime;
    private BSONObject extraInfo;

    public QuotaSyncInfo(BSONObject bson) {
        this.type = BsonUtils.getStringChecked(bson, FieldName.QuotaSync.TYPE);
        this.name = BsonUtils.getStringChecked(bson, FieldName.QuotaSync.NAME);
        this.statisticsSize = BsonUtils.getNumberChecked(bson, FieldName.QuotaSync.STATISTICS_SIZE)
                .longValue();
        this.statisticsObjects = BsonUtils
                .getNumberChecked(bson, FieldName.QuotaSync.STATISTICS_OBJECTS).longValue();
        this.isFirstSync = BsonUtils.getBooleanChecked(bson, FieldName.QuotaSync.IS_FIRST_SYNC);
        this.syncRoundNumber = BsonUtils
                .getNumberChecked(bson, FieldName.QuotaSync.SYNC_ROUND_NUMBER).intValue();
        this.quotaRoundNumber = BsonUtils
                .getNumberChecked(bson, FieldName.QuotaSync.QUOTA_ROUND_NUMBER).intValue();
        this.status = BsonUtils.getStringChecked(bson, FieldName.QuotaSync.STATUS);
        Number temp = BsonUtils.getNumber(bson, FieldName.QuotaSync.AGREEMENT_TIME);
        if (temp != null) {
            this.agreementTime = temp.longValue();
        }
        this.extraInfo = BsonUtils.getBSON(bson, FieldName.QuotaSync.EXTRA_INFO);
        this.expireTime = BsonUtils.getNumberChecked(bson, FieldName.QuotaSync.EXPIRE_TIME)
                .longValue();
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

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getStatisticsSize() {
        return statisticsSize;
    }

    public void setStatisticsSize(long statisticsSize) {
        this.statisticsSize = statisticsSize;
    }

    public long getStatisticsObjects() {
        return statisticsObjects;
    }

    public void setStatisticsObjects(long statisticsObjects) {
        this.statisticsObjects = statisticsObjects;
    }

    public int getSyncRoundNumber() {
        return syncRoundNumber;
    }

    public void setSyncRoundNumber(int syncRoundNumber) {
        this.syncRoundNumber = syncRoundNumber;
    }

    public String getStatus() {
        return status;
    }

    public boolean isFirstSync() {
        return isFirstSync;
    }

    public void setFirstSync(boolean firstSync) {
        isFirstSync = firstSync;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAgreementTime() {
        return agreementTime;
    }

    public void setAgreementTime(Long agreementTime) {
        this.agreementTime = agreementTime;
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
        return "QuotaSyncInfo{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", statisticsSize=" + statisticsSize + ", expireTime=" + expireTime
                + ", statisticsObjects=" + statisticsObjects + ", syncRoundNumber=" + syncRoundNumber
                + ", quotaRoundNumber=" + quotaRoundNumber + ", isFirstSync=" + isFirstSync
                + ", status='" + status + '\'' + ", agreementTime=" + agreementTime + ", extraInfo="
                + extraInfo + '}';
    }
}
