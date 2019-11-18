package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmRoleBasicInfo {
    @JsonProperty("role_id")
    private String roleId;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("description")
    private String description;

    public OmRoleBasicInfo() {
    }

    public OmRoleBasicInfo(String roleId, String roleName, String description) {
        super();
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
