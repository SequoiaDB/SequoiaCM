package com.sequoiacm.infrastructure.config.core.msg.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@BusinessType(ScmBusinessTypeDefine.ROLE)
public class RoleNotifyOption implements NotifyOption {

    @JsonProperty(ScmRestArgDefine.ROLE_CONF_ROLENAME)
    private String roleName;

    public RoleNotifyOption(String roleName) {
        this.roleName = roleName;
    }

    public RoleNotifyOption() {
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public Version getBusinessVersion() {
        return null;
    }

    @Override
    public String getBusinessName() {
        return roleName;
    }

    @Override
    public String toString() {
        return "RoleNotifyOption{" + "roleName='" + roleName + '\'' + '}';
    }
}
