package com.sequoiacm.infrastructure.config.core.msg;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface BsonConverter {
    Config convertToConfig(BSONObject config);

    ConfigFilter convertToConfigFilter(BSONObject configFilter) throws ScmConfigException;

    NotifyOption convertToNotifyOption(EventType type, BSONObject notifyOption);

    ConfigUpdator convertToConfigUpdator(BSONObject configUpdator);

    VersionFilter convertToVersionFilter(BSONObject versionFilter);

    Version convertToVersion(BSONObject version);

    String getConfigName();
}
