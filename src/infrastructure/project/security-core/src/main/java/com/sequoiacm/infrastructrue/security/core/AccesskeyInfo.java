package com.sequoiacm.infrastructrue.security.core;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

public class AccesskeyInfo {
    public static final String JSON_FIELD_ACCESSKEY = "accesskey";
    public static final String JSON_FIELD_SECRETKEY = "secretkey";
    public static final String JSON_FIELD_USERNAME = "username";
    private String accesskey;
    private String secretkey;
    private String username;

    public AccesskeyInfo(String accesskey, String secretkey, String username) {
        super();
        this.accesskey = accesskey;
        this.secretkey = secretkey;
        this.username = username;
    }

    public AccesskeyInfo(BSONObject bson) {
        this(BsonUtils.getStringChecked(bson, JSON_FIELD_ACCESSKEY),
                BsonUtils.getStringChecked(bson, JSON_FIELD_SECRETKEY),
                BsonUtils.getStringChecked(bson, JSON_FIELD_USERNAME));
    }

    public String getAccesskey() {
        return accesskey;
    }

    public String getSecretkey() {
        return secretkey;
    }

    public String getUsername() {
        return username;
    }

}
