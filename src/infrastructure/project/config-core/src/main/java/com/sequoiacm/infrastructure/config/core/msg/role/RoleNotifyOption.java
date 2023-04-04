package com.sequoiacm.infrastructure.config.core.msg.role;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;

public class RoleNotifyOption implements NotifyOption {

    private String roleName;
    private EventType eventType;

    public RoleNotifyOption(String roleName, EventType type) {
        this.roleName = roleName;
        this.eventType = type;
    }


    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.ROLE_CONF_ROLENAME, roleName);
        return obj;
    }

    @Override
    public String toString() {
        return "RoleNotifyOption{" + "roleName='" + roleName + '\'' + ", eventType=" + eventType + '}';
    }
}
