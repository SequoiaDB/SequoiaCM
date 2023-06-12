package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

@BusinessType(ScmBusinessTypeDefine.QUOTA)
public class QuotaUpdater implements ConfigUpdater {

    @JsonProperty(FieldName.Quota.TYPE)
    private String type;

    @JsonProperty(FieldName.Quota.NAME)
    private String name;

    @JsonProperty(ScmRestArgDefine.QUOTA_CONF_MATCHER)
    private BSONObject matcher = new BasicBSONObject();

    @JsonProperty(ScmRestArgDefine.QUOTA_CONF_UPDATOR)
    private Updater updater = new Updater();

    public QuotaUpdater() {
    }

    public QuotaUpdater(String type, String name, Long maxSize, Long maxObjects, Boolean enable,
                        BSONObject extraMatcher) {
        updater.setMaxSize(maxSize);
        updater.setMaxObjects(maxObjects);
        updater.setEnable(enable);

        this.type = type;
        this.name = name;
        if (extraMatcher != null) {
            this.matcher.putAll(extraMatcher);
        }
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

    public void setEnable(Boolean enable) {
        this.updater.setEnable(enable);
    }

    public Boolean getEnable() {
        return updater.getEnable();
    }

    public Long getMaxSize() {
        return updater.getMaxSize();
    }

    public Long getMaxObjects() {
        return updater.getMaxObjects();
    }

    public BSONObject getExtraInfo() {
        return updater.getExtraInfo();
    }

    public Integer getQuotaRoundNumber() {
        return updater.getQuotaRoundNumber();
    }

    public Long getUsedObjects() {
        return updater.getUsedObjects();
    }

    public Long getUsedSize() {
        return updater.getUsedSize();
    }

    public BSONObject getMatcher() {
        return matcher;
    }

    public void setMatcher(BSONObject matcher) {
        this.matcher = matcher;
    }

    public Updater getUpdater() {
        return updater;
    }

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }

    public void setUsedSize(Long usedSize) {
        this.updater.setUsedSize(usedSize);
    }

    public void setQuotaRoundNumber(Integer roundNumber) {
        this.updater.setQuotaRoundNumber(roundNumber);
    }

    public void setUsedObjects(Long usedObjects) {
        this.updater.setUsedObjects(usedObjects);
    }


    @Override
    public String toString() {
        return "QuotaUpdater{" + "type='" + type + '\'' + ", name='" + name + '\'' + ", matcher="
                + matcher + ", updater=" + updater + '}';
    }
}

class Updater {
    @JsonProperty(FieldName.Quota.MAX_SIZE)
    private Long maxSize;

    @JsonProperty(FieldName.Quota.MAX_OBJECTS)
    private Long maxObjects;

    @JsonProperty(FieldName.Quota.QUOTA_ROUND_NUMBER)
    private Integer quotaRoundNumber;

    @JsonProperty(FieldName.Quota.ENABLE)
    private Boolean enable;

    @JsonProperty(FieldName.Quota.EXTRA_INFO)
    private BSONObject extraInfo;

    @JsonProperty(FieldName.Quota.USED_OBJECTS)
    private Long usedObjects;

    @JsonProperty(FieldName.Quota.USED_SIZE)
    private Long usedSize;

    public Updater() {
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public Long getMaxObjects() {
        return maxObjects;
    }

    public void setMaxObjects(Long maxObjects) {
        this.maxObjects = maxObjects;
    }

    public Integer getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    public void setQuotaRoundNumber(Integer quotaRoundNumber) {
        this.quotaRoundNumber = quotaRoundNumber;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }

    public Long getUsedObjects() {
        return usedObjects;
    }

    public void setUsedObjects(Long usedObjects) {
        this.usedObjects = usedObjects;
    }

    public Long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(Long usedSize) {
        this.usedSize = usedSize;
    }

    @Override
    public String toString() {
        return "Updater{" + "maxSize=" + maxSize + ", maxObjects=" + maxObjects
                + ", quotaRoundNumber=" + quotaRoundNumber + ", enable=" + enable + ", extraInfo="
                + extraInfo + ", usedObjects=" + usedObjects + ", usedSize=" + usedSize + '}';
    }
}
