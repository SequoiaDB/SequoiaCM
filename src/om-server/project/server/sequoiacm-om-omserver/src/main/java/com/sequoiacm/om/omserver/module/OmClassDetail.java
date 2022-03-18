package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class OmClassDetail extends OmClassBasic {

    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("update_time")
    private Date updateTime;

    @JsonProperty("attrs")
    private List<OmAttributeBasicInfo> attrList;

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<OmAttributeBasicInfo> getAttrList() {
        return attrList;
    }

    public void setAttrList(List<OmAttributeBasicInfo> attrList) {
        this.attrList = attrList;
    }
}
