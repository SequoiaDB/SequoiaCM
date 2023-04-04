package com.sequoiacm.infrastructure.config.core.msg.user;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class UserConfig implements Config {

    private String username;

    public UserConfig() {

    }

    public UserConfig(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject userConfigObj = new BasicBSONObject();
        userConfigObj.put(FieldName.User.FIELD_USERNAME, username);
        return userConfigObj;
    }


}
