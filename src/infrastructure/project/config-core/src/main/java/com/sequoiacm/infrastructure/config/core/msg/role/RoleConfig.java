package com.sequoiacm.infrastructure.config.core.msg.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;
@BusinessType(ScmBusinessTypeDefine.ROLE)
public class RoleConfig implements Config {

    @JsonProperty(FieldName.Role.FIELD_ROLE_NAME)
    private String name;

    public RoleConfig() {

    }

    public RoleConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String username) {
        this.name = username;
    }

    @Override
    public String getBusinessName() {
        return name;
    }
}
