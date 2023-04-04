package com.sequoiacm.infrastructure.config.core.msg.user;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;

public class UserNotifyOption implements NotifyOption {

    private String username;
    private EventType eventType;

    public UserNotifyOption(String username, EventType type) {
        this.username = username;
        this.eventType = type;
    }


    @Override
    public EventType getEventType() {
        return eventType;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.USER_CONF_USERNAME, username);
        return obj;
    }

    @Override
    public String toString() {
        return "UserNotifyOption{" + "username='" + username + '\'' + ", eventType=" + eventType + '}';
    }
}
