package com.sequoiacm.infrastructure.config.core.msg.user;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class UserFilter implements ConfigFilter {

    private String username;

    public UserFilter(String username) {
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
        BasicBSONObject obj = new BasicBSONObject();
        if (username != null) {
            obj.put(ScmRestArgDefine.USER_CONF_USERNAME, username);
        }
        return obj;
    }
}
