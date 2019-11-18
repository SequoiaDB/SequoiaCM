package com.sequoiacm.perf.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthVo {
    @JsonProperty("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
