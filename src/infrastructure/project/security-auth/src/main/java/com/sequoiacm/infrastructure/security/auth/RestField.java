package com.sequoiacm.infrastructure.security.auth;

public final class RestField {
    private RestField(){
    }

    public static final String SESSION_ATTRIBUTE = "x-auth-token";
    public static final String USER_ATTRIBUTE = "x-auth-user";
    public static final String USER_DETAILS = "user_details";
}
