package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Owner {
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String JSON_KEY_USERID = "ID";

    @JsonProperty(DISPLAY_NAME)
    private String userName;
    @JsonProperty(JSON_KEY_USERID)
    private String userId;

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
