package com.sequoiacm.cloud.adminserver.model;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

public class QuotaSyncInfo {

    private String type;
    private String name;
    private Long statisticsSize;
    private Long statisticsObjects;
    private String status;
    private String errorMsg;
    private int syncRoundNumber;
    private int quotaRoundNumber;
    private boolean isFirstSync;
    private long beginTime;
    private Long endTime;
    private Long agreementTime;
    private BSONObject statisticsDetail;
    private BSONObject extraInfo;
    private long updateTime;
    private long expireTime;

    public QuotaSyncInfo(BSONObject bsonObject) {
        this.type = BsonUtils.getStringChecked(bsonObject, FieldName.QuotaSync.TYPE);
        this.name = BsonUtils.getStringChecked(bsonObject, FieldName.QuotaSync.NAME);
        this.statisticsSize = BsonUtils
                .getNumberChecked(bsonObject, FieldName.QuotaSync.STATISTICS_SIZE).longValue();
        this.statisticsObjects = BsonUtils
                .getNumberChecked(bsonObject, FieldName.QuotaSync.STATISTICS_OBJECTS).longValue();
        this.status = BsonUtils.getStringChecked(bsonObject, FieldName.QuotaSync.STATUS);
        this.errorMsg = BsonUtils.getString(bsonObject, FieldName.QuotaSync.ERROR_MSG);
        this.syncRoundNumber = BsonUtils
                .getNumberChecked(bsonObject, FieldName.QuotaSync.SYNC_ROUND_NUMBER).intValue();
        this.quotaRoundNumber = BsonUtils
                .getNumberChecked(bsonObject, FieldName.QuotaSync.QUOTA_ROUND_NUMBER).intValue();
        this.isFirstSync = BsonUtils.getBooleanChecked(bsonObject,
                FieldName.QuotaSync.IS_FIRST_SYNC);
        this.beginTime = BsonUtils.getNumberChecked(bsonObject, FieldName.QuotaSync.BEGIN_TIME)
                .longValue();
        this.expireTime = BsonUtils.getNumberChecked(bsonObject, FieldName.QuotaSync.EXPIRE_TIME)
                .longValue();
        Number temp = BsonUtils.getNumber(bsonObject, FieldName.QuotaSync.END_TIME);
        if (temp != null) {
            this.endTime = temp.longValue();
        }
        temp = BsonUtils.getNumber(bsonObject, FieldName.QuotaSync.AGREEMENT_TIME);
        if (temp != null) {
            this.agreementTime = temp.longValue();
        }
        this.statisticsDetail = BsonUtils.getBSON(bsonObject,
                FieldName.QuotaSync.STATISTICS_DETAIL);
        this.extraInfo = BsonUtils.getBSON(bsonObject, FieldName.QuotaSync.EXTRA_INFO);
        this.updateTime = BsonUtils.getNumberChecked(bsonObject, FieldName.QuotaSync.UPDATE_TIME)
                .longValue();

    }

    public QuotaSyncInfo() {
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

    public Long getStatisticsSize() {
        return statisticsSize;
    }

    public void setStatisticsSize(Long statisticsSize) {
        this.statisticsSize = statisticsSize;
    }

    public Long getStatisticsObjects() {
        return statisticsObjects;
    }

    public void setStatisticsObjects(Long statisticsObjects) {
        this.statisticsObjects = statisticsObjects;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public int getSyncRoundNumber() {
        return syncRoundNumber;
    }

    public void setSyncRoundNumber(int syncRoundNumber) {
        this.syncRoundNumber = syncRoundNumber;
    }

    public boolean isFirstSync() {
        return isFirstSync;
    }

    public void setFirstSync(boolean firstSync) {
        isFirstSync = firstSync;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getAgreementTime() {
        return agreementTime;
    }

    public void setAgreementTime(Long agreementTime) {
        this.agreementTime = agreementTime;
    }

    public BSONObject getStatisticsDetail() {
        return statisticsDetail;
    }

    public void setStatisticsDetail(BSONObject statisticsDetail) {
        this.statisticsDetail = statisticsDetail;
    }

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
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
                + ", statisticsSize=" + statisticsSize + ", statisticsObjects=" + statisticsObjects
                + ", status='" + status + '\'' + ", errorMsg='" + errorMsg + '\''
                + ", syncRoundNumber=" + syncRoundNumber + ", quotaRoundNumber=" + quotaRoundNumber
                + ", isFirstSync=" + isFirstSync + ", beginTime=" + beginTime + ", endTime="
                + endTime + ", agreementTime=" + agreementTime + ", statisticsDetail="
                + statisticsDetail + ", extraInfo=" + extraInfo + ", updateTime=" + updateTime
                + ", expireTime=" + expireTime + '}';
    }
}
