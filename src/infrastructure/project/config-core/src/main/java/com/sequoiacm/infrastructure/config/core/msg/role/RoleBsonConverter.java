package com.sequoiacm.infrastructure.config.core.msg.role;

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
import com.sequoiacm.infrastructure.config.core.msg.user.UserNotifyOption;

@Converter
public class RoleBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        RoleConfig roleConfig = new RoleConfig();
        roleConfig.setName(BsonUtils.getStringChecked(config, ScmRestArgDefine.ROLE_CONF_ROLENAME));
        return roleConfig;
    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) {
        String roleName = BsonUtils.getString(configFilter, ScmRestArgDefine.ROLE_CONF_ROLENAME);
        return new RoleFilter(roleName);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject configOption) {
        String roleName = BsonUtils.getStringChecked(configOption,
                ScmRestArgDefine.ROLE_CONF_ROLENAME);
        return new UserNotifyOption(roleName, type);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdatorObj) {
        RoleUpdator roleUpdator = new RoleUpdator();
        String roleName = BsonUtils.getStringChecked(configUpdatorObj,
                ScmRestArgDefine.ROLE_CONF_ROLENAME);
        roleUpdator.setName(roleName);
        return roleUpdator;
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
        return ScmConfigNameDefine.ROLE;
    }
}
