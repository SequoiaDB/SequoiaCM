package com.sequoiacm.infrastructure.config.core.msg.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;


import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;

@BusinessType(ScmBusinessTypeDefine.ROLE)
public class RoleUpdater implements ConfigUpdater {

    @JsonProperty(ScmRestArgDefine.ROLE_CONF_ROLENAME)
    private String name;

    public RoleUpdater() {
    }

    public RoleUpdater(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
