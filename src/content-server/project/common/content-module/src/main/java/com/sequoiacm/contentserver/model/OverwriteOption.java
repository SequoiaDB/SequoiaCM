package com.sequoiacm.contentserver.model;

public class OverwriteOption {
    private String sessionId;
    private String userDetail;

    public OverwriteOption(String sessionId, String userDetail) {
        this.sessionId = sessionId;
        this.userDetail = userDetail;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserDetail() {
        return userDetail;
    }
}
