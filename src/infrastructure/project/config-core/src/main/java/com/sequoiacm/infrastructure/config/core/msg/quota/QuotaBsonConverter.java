package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.Converter;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import org.bson.BSONObject;

@Converter
public class QuotaBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        QuotaConfig quotaConfig = new QuotaConfig();
        quotaConfig.setEnable(BsonUtils.getBooleanChecked(config, FieldName.Quota.ENABLE));
        quotaConfig.setType(BsonUtils.getStringChecked(config, FieldName.Quota.TYPE));
        quotaConfig.setName(BsonUtils.getStringChecked(config, FieldName.Quota.NAME));
        quotaConfig.setMaxObjects(
                BsonUtils.getNumberChecked(config, FieldName.Quota.MAX_OBJECTS).longValue());
        quotaConfig.setMaxSize(
                BsonUtils.getNumberChecked(config, FieldName.Quota.MAX_SIZE).longValue());
        quotaConfig.setQuotaRoundNumber(
                BsonUtils.getNumberChecked(config, FieldName.Quota.QUOTA_ROUND_NUMBER).intValue());
        quotaConfig.setExtraInfo(BsonUtils.getBSON(config, FieldName.Quota.EXTRA_INFO));
        return quotaConfig;
    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) throws ScmConfigException {
        String type = BsonUtils.getString(configFilter, FieldName.Quota.TYPE);
        String name = BsonUtils.getString(configFilter, FieldName.Quota.NAME);
        return new QuotaFilter(type, name);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject notifyOption) {
        Integer version = BsonUtils.getInteger(notifyOption, ScmRestArgDefine.QUOTA_CONF_VERSION);
        String quotaType = BsonUtils.getStringChecked(notifyOption, FieldName.Quota.TYPE);
        String name = BsonUtils.getStringChecked(notifyOption, FieldName.Quota.NAME);
        return new QuotaNotifyOption(quotaType, name, version, type);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject bsonObject) {
        QuotaUpdator quotaUpdator = new QuotaUpdator();
        quotaUpdator.setName(BsonUtils.getStringChecked(bsonObject, FieldName.Quota.NAME));
        quotaUpdator.setType(BsonUtils.getStringChecked(bsonObject, FieldName.Quota.TYPE));
        BSONObject updatorBson = BsonUtils.getBSON(bsonObject, ScmRestArgDefine.QUOTA_CONF_UPDATOR);
        if (updatorBson != null) {
            Object temp = updatorBson.get(FieldName.Quota.MAX_OBJECTS);
            if (temp != null) {
                quotaUpdator.setMaxObjects(((Number) temp).longValue());
            }
            temp = updatorBson.get(FieldName.Quota.MAX_SIZE);
            if (temp != null) {
                quotaUpdator.setMaxSize(((Number) temp).longValue());
            }
            temp = updatorBson.get(FieldName.Quota.ENABLE);
            if (temp != null) {
                quotaUpdator.setEnable((Boolean) temp);
            }
            temp = updatorBson.get(FieldName.Quota.EXTRA_INFO);
            if (temp != null) {
                quotaUpdator.setExtraInfo((BSONObject) temp);
            }
            temp = updatorBson.get(FieldName.Quota.QUOTA_ROUND_NUMBER);
            if (temp != null) {
                quotaUpdator.setQuotaRoundNumber(((Number) temp).intValue());
            }
            temp = updatorBson.get(FieldName.Quota.USED_SIZE);
            if (temp != null) {
                quotaUpdator.setUsedSize(((Number) temp).longValue());
            }
            temp = updatorBson.get(FieldName.Quota.USED_OBJECTS);
            if (temp != null) {
                quotaUpdator.setUsedObjects(((Number) temp).longValue());
            }
        }
        quotaUpdator.setMatcher(BsonUtils.getBSON(bsonObject, ScmRestArgDefine.QUOTA_CONF_MATCHER));
        return quotaUpdator;
    }

    @Override
    public VersionFilter convertToVersionFilter(BSONObject versionFilterBson) {
        QuotaVersionFilter quotaVersionFilter = new QuotaVersionFilter();
        String temp = BsonUtils.getString(versionFilterBson, FieldName.Quota.TYPE);
        if (temp != null) {
            quotaVersionFilter.setType(temp);
        }
        temp = BsonUtils.getString(versionFilterBson, FieldName.Quota.NAME);
        if (temp != null) {
            quotaVersionFilter.setName(temp);
        }
        return quotaVersionFilter;
    }

    @Override
    public Version convertToVersion(BSONObject version) {
        return new DefaultVersion(version);
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.QUOTA;
    }
}
