package com.sequoiacm.infrastructure.security.auth;

public final class RestField {
    private RestField() {
    }

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String SESSION_ATTRIBUTE = "x-auth-token";
    public static final String USER_ATTRIBUTE = "x-auth-user";
    public static final String USER_ATTRIBUTE_USER_NAME = "x-auth-user-name";
    public static final String USER_DETAILS = "user_details";

    public static final String ACCESSKEY = "accesskey";
    public static final String SECRETKEY = "secretkey";
    public static final String SIGNATURE_INFO = "signature_info";
    public static final String SIGNATURE_INFO_ALGOTHM = "algothm";
    public static final String SIGNATURE_INFO_ACCESSKEY = "accesskey";
    public static final String SIGNATURE_INFO_SINAGTURE = "signature";
    public static final String SIGNATURE_INFO_SINAGTURE_ENCODER = "signature_encoder";
    public static final String SIGNATURE_INFO_SECREKEY_PREFIX = "secretkey_prefix";
    public static final String SIGNATURE_INFO_STRING_TO_SIGN = "string_to_sign";

}
