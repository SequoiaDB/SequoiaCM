package com.sequoiacm.infrastructure.config.core.msg.node;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class NodeNotifyOption implements NotifyOption {
    private String nodeName;
    private Integer version;
    private EventType eventType;

    public NodeNotifyOption(String nodeName, Integer version, EventType eventType) {
        this.nodeName = nodeName;
        this.version = version;
        this.eventType = eventType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public DefaultVersion getVersion() {
        if (eventType == EventType.DELTE) {
            return new DefaultVersion(ScmConfigNameDefine.NODE, nodeName, -1);
        }
        return new DefaultVersion(ScmConfigNameDefine.NODE, nodeName, version);
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "nodeName=" + nodeName + ",eventType=" + eventType + ",version=" + version;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.NODE_CONF_NODENAME, nodeName);
        obj.put(ScmRestArgDefine.NODE_CONF_NODEVERSION, version);
        return obj;
    }

}
