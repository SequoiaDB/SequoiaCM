package com.sequoiacm.contentserver.model;

public class SessionInfoWrapper {
    private String sessionId;
    private String userDetail;

    public SessionInfoWrapper(String sessionId, String userDetail) {
        this.sessionId = sessionId;
        this.userDetail = userDetail;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserDetail() {
        return userDetail;
    }

    @Override
    public String toString() {
        return "ForwardAttribute{" + "sessionId='" + sessionId + '\'' + ", userDetail='"
                + userDetail + '\'' + '}';
    }
}
