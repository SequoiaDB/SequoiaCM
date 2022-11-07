package com.sequoiacm.client.element;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

public class ScmAccesskeyInfo {
    public static final String JSON_FIELD_ACCESSKEY = "accesskey";
    public static final String JSON_FIELD_SECRETKEY = "secretkey";
    public static final String JSON_FIELD_USERNAME = "username";

    private String accesskey;
    private String secretkey;
    private String username;

    public ScmAccesskeyInfo(String accesskey, String secretkey, String username) {
        super();
        this.accesskey = accesskey;
        this.secretkey = secretkey;
        this.username = username;
    }

    public ScmAccesskeyInfo(BSONObject bson) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("username:" + username).append(",").append("accesskey:" + accesskey).append(",")
                .append("secretkey:" + secretkey);
        return sb.toString();
    }
}
