package com.sequoiacm.infrastructure.config.core.msg.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
@BusinessType(ScmBusinessTypeDefine.USER)
public class UserUpdater implements ConfigUpdater {

    @JsonProperty(ScmRestArgDefine.USER_CONF_USERNAME)
    private String username;

    public UserUpdater() {
    }

    public UserUpdater(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
