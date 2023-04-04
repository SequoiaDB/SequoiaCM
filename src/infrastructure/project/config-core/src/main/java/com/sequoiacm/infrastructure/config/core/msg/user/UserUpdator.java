package com.sequoiacm.infrastructure.config.core.msg.user;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;

public class UserUpdator implements ConfigUpdator {

    private String username;

    public UserUpdator() {

    }

    public UserUpdator(String username) {
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
        BSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.USER_CONF_USERNAME, username);
        BSONObject updator = new BasicBSONObject();
        updator.put(ScmRestArgDefine.USER_CONF_USERNAME, username);
        obj.put(ScmRestArgDefine.SITE_CONF_UPDATOR, updator);
        return obj;
    }
}
