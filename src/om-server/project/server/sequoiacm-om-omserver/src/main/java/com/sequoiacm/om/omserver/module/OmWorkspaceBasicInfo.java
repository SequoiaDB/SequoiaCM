package com.sequoiacm.om.omserver.module;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmWorkspaceBasicInfo {
    @JsonProperty("name")
    private String name;

    @JsonProperty("create_time")
    private Date createTime;

    @JsonProperty("create_user")
    private String createUser;

    @JsonProperty("description")
    private String description;

    @JsonProperty("site_list")
    private List<String> siteList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSiteList() {
        return siteList;
    }

    public void setSiteList(List<String> siteList) {
        this.siteList = siteList;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OmWorkspaceBasicInfo)) {
            return false;
        }
        OmWorkspaceBasicInfo other = (OmWorkspaceBasicInfo) obj;
        if (createTime == null) {
            if (other.createTime != null) {
                return false;
            }
        }
        else if (!createTime.equals(other.createTime)) {
            return false;
        }
        if (createUser == null) {
            if (other.createUser != null) {
                return false;
            }
        }
        else if (!createUser.equals(other.createUser)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        }
        else if (!description.equals(other.description)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
