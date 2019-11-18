package com.sequoiacm.om.omserver.module;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmUserInfo {
    @JsonProperty("roles")
    private List<OmRoleBasicInfo> roles;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("user_type")
    private String userType;
    @JsonProperty("enable")
    private boolean enable;
    @JsonProperty("user_id")
    private String userId;

    public List<OmRoleBasicInfo> getRoles() {
        return roles;
    }

    public void setRoles(List<OmRoleBasicInfo> roles) {
        this.roles = roles;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
