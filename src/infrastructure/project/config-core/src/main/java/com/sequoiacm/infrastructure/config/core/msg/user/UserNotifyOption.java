package com.sequoiacm.infrastructure.config.core.msg.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@BusinessType(ScmBusinessTypeDefine.USER)
public class UserNotifyOption implements NotifyOption {

    @JsonProperty(ScmRestArgDefine.USER_CONF_USERNAME)
    private String username;

    public UserNotifyOption(String username) {
        this.username = username;
    }

    public UserNotifyOption() {
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String getBusinessName() {
        return username;
    }

    @Override
    public Version getBusinessVersion() {
        return null;
    }

    @Override
    public String toString() {
        return "UserNotifyOption{" + "username='" + username + '\'' + '}';
    }
}
