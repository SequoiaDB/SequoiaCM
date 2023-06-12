package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import org.bson.BSONObject;

@BusinessType(ScmBusinessTypeDefine.QUOTA)
public class QuotaConfig implements Config {

    @JsonIgnore
    private static final String SEPARATOR = "-";
    @JsonProperty(FieldName.Quota.TYPE)
    private String type;

    @JsonProperty(FieldName.Quota.NAME)
    private String name;

    @JsonProperty(FieldName.Quota.MAX_SIZE)
    private long maxSize = -1;

    @JsonProperty(FieldName.Quota.MAX_OBJECTS)
    private long maxObjects = -1;

    @JsonProperty(FieldName.Quota.ENABLE)
    private boolean enable;

    @JsonProperty(FieldName.Quota.QUOTA_ROUND_NUMBER)
    // 每次重新打开限额时，递增该数字
    private int quotaRoundNumber;

    @JsonProperty(FieldName.Quota.EXTRA_INFO)
    private BSONObject extraInfo;

    public QuotaConfig(String type, String name, long maxSize, long maxObjects, boolean enable,
            int quotaRoundNumber, BSONObject extraInfo) {
        this.type = type;
        this.name = name;
        this.maxSize = maxSize;
        this.maxObjects = maxObjects;
        this.enable = enable;
        this.quotaRoundNumber = quotaRoundNumber;
        this.extraInfo = extraInfo;
    }

    public QuotaConfig() {
    }

    public static String getTypeFromBusinessName(String businessName) {
        return businessName.substring(0, businessName.indexOf(SEPARATOR));
    }

    public static String getNameFromBusinessName(String businessName) {
        return businessName.substring(businessName.indexOf(SEPARATOR) + 1);
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

    @Override
    public String getBusinessName() {
        return toBusinessName(type, name);
    }

    public static String toBusinessName(String type, String name) {
        return type + SEPARATOR + name;
    }

    public int getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    public void setQuotaRoundNumber(int quotaRoundNumber) {
        this.quotaRoundNumber = quotaRoundNumber;
    }

    @Override
    public String toString() {
        return "QuotaConfig{" + "type='" + type + '\'' + ", name='" + name + '\'' + ", maxSize="
                + maxSize + ", maxObjects=" + maxObjects + ", enable=" + enable + ", quotaRoundNumber="
                + quotaRoundNumber + ", extraInfo=" + extraInfo + '}';
    }

}
