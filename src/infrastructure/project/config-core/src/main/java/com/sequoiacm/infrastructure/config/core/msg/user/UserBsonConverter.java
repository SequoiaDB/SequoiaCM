package com.sequoiacm.infrastructure.config.core.msg.user;

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
public class UserBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        UserConfig userConfig = new UserConfig();
        userConfig.setUsername(
                BsonUtils.getStringChecked(config, ScmRestArgDefine.USER_CONF_USERNAME));
        return userConfig;
    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) {
        String username = BsonUtils.getString(configFilter, ScmRestArgDefine.USER_CONF_USERNAME);
        return new UserFilter(username);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject configOption) {
        String username = BsonUtils.getStringChecked(configOption,
                ScmRestArgDefine.USER_CONF_USERNAME);
        return new UserNotifyOption(username, type);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdatorObj) {
        UserUpdator userUpdator = new UserUpdator();
        String username = BsonUtils.getStringChecked(configUpdatorObj,
                ScmRestArgDefine.USER_CONF_USERNAME);
        userUpdator.setUsername(username);
        return userUpdator;
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
        return ScmConfigNameDefine.USER;
    }
}
