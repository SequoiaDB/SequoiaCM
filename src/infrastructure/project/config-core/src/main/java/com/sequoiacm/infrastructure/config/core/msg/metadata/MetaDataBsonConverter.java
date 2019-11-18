package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.Converter;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Converter
public class MetaDataBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        return new MetaDataConfig(config);
    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) {
        return new MetaDataConfigFilter(configFilter);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject notifyOption) {
        Integer version = BsonUtils.getInteger(notifyOption, ScmRestArgDefine.META_DATA_VERSION);
        String wsName = BsonUtils.getStringChecked(notifyOption,
                ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        return new MetaDataNotifyOption(wsName, type, version);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdator) {
        return new MetaDataConfigUpdator(configUpdator);
    }

    @Override
    public VersionFilter convertToVersionFilter(BSONObject versionFilter) {
        return new DefaultVersionFilter(versionFilter);
    }

    @Override
    public Version convertToVersion(BSONObject version) {
        return new DefaultVersion(version);
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.META_DATA;
    }

}
