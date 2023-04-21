package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class QuotaUpdator implements ConfigUpdator {

    private String type;
    private String name;
    private Long maxSize;
    private Long maxObjects;
    private Integer quotaRoundNumber;
    private Boolean enable;
    private BSONObject extraInfo;

    private Long usedObjects;
    private Long usedSize;

    private BSONObject matcher;

    public QuotaUpdator() {
    }

    public QuotaUpdator(String type, String name, Long maxSize, Long maxObjects,
            Boolean enable, BSONObject matcher) {
        this.type = type;
        this.name = name;
        this.maxSize = maxSize;
        this.maxObjects = maxObjects;
        this.enable = enable;
        this.matcher = matcher;
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

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
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

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }

    public Integer getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    public void setQuotaRoundNumber(Integer quotaRoundNumber) {
        this.quotaRoundNumber = quotaRoundNumber;
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

    public BSONObject getMatcher() {
        return matcher;
    }

    public void setMatcher(BSONObject matcher) {
        this.matcher = matcher;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Quota.TYPE, type);
        obj.put(FieldName.Quota.NAME, name);

        BSONObject updator = new BasicBSONObject();
        if (maxObjects != null) {
            updator.put(FieldName.Quota.MAX_OBJECTS, maxObjects);
        }
        if (maxSize != null) {
            updator.put(FieldName.Quota.MAX_SIZE, maxSize);
        }
        if (enable != null) {
            updator.put(FieldName.Quota.ENABLE, enable);
        }
        if (extraInfo != null) {
            updator.put(FieldName.Quota.EXTRA_INFO, extraInfo);
        }
        if (quotaRoundNumber != null) {
            updator.put(FieldName.Quota.QUOTA_ROUND_NUMBER, quotaRoundNumber);
        }
        if (usedObjects != null) {
            updator.put(FieldName.Quota.USED_OBJECTS, usedObjects);
        }
        if (usedSize != null) {
            updator.put(FieldName.Quota.USED_SIZE, usedSize);
        }
        obj.put(ScmRestArgDefine.QUOTA_CONF_UPDATOR, updator);
        obj.put(ScmRestArgDefine.QUOTA_CONF_MATCHER, matcher);
        return obj;
    }

}
