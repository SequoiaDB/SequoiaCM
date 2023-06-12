package com.sequoiacm.infrastructure.config.core.msg.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.Config;

@BusinessType(ScmBusinessTypeDefine.USER)
public class UserConfig implements Config {

    @JsonProperty(FieldName.User.FIELD_USERNAME)
    private String username;

    public UserConfig() {
    }

    public UserConfig(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getBusinessName() {
        return username;
    }
}
