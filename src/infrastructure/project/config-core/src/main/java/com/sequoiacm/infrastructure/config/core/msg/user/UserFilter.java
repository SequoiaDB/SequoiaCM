package com.sequoiacm.infrastructure.config.core.msg.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;


import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
@BusinessType(ScmBusinessTypeDefine.USER)
public class UserFilter implements ConfigFilter {

    @JsonProperty(ScmRestArgDefine.USER_CONF_USERNAME)
    private String username;

    public UserFilter(String username) {
        this.username = username;
    }

    public UserFilter() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
