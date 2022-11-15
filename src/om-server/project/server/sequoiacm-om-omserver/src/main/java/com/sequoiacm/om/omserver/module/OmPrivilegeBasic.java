package com.sequoiacm.om.omserver.module;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmPrivilegeBasic {

    @JsonProperty("id")
    private String id;

    @JsonProperty("role_type")
    private String roleType;

    @JsonProperty("role_id")
    private String roleId;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("privileges")
    private List<String> privilegeList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public List<String> getPrivilegeList() {
        return privilegeList;
    }

    public void setPrivilegeList(List<String> privilegeList) {
        this.privilegeList = privilegeList;
    }
}
