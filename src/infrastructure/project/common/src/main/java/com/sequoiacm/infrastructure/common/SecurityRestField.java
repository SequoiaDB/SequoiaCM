package com.sequoiacm.infrastructure.common;

public final class SecurityRestField {
    private SecurityRestField() {
    }

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    // the new version of local user Login header field
    public static final String SIGNATURE_DATE = "date";

    public static final String SESSION_ATTRIBUTE = "x-auth-token";
    public static final String USER_ATTRIBUTE = "x-auth-user";
    public static final String USER_ATTRIBUTE_USER_NAME = "x-auth-user-name";
    public static final String USER_DETAILS = "user_details";
    public static final String USER_INFO_WRAPPER = "user_info_wrapper";

    public static final String ACCESSKEY = "accesskey";
    public static final String SECRETKEY = "secretkey";
    public static final String SIGNATURE_INFO = "signature_info";
    public static final String SIGNATURE_INFO_ALGORITHM = "algorithm";
    public static final String SIGNATURE_INFO_ACCESSKEY = "accesskey";
    public static final String SIGNATURE_INFO_SINAGTURE = "signature";
    public static final String SIGNATURE_INFO_SINAGTURE_ENCODER = "signature_encoder";
    public static final String SIGNATURE_INFO_SECREKEY_PREFIX = "secretkey_prefix";
    public static final String SIGNATURE_INFO_STRING_TO_SIGN = "string_to_sign";
}
