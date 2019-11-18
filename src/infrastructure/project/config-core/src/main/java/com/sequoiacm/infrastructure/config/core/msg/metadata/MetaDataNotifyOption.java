package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class MetaDataNotifyOption implements NotifyOption {
    private String wsName;
    private EventType type;
    private Integer version;

    public MetaDataNotifyOption(String wsName, EventType type, Integer version) {
        this.wsName = wsName;
        this.type = type;
        this.version = version;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    @Override
    public DefaultVersion getVersion() {
        if (type == EventType.DELTE) {
            return new DefaultVersion(ScmConfigNameDefine.META_DATA, wsName, -1);
        }
        return new DefaultVersion(ScmConfigNameDefine.META_DATA, wsName, version);
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public EventType getEventType() {
        return type;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.META_DATA_VERSION, version);
        obj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        return obj;
    }

    @Override
    public String toString() {
        return "MetaDataNotifyOption [wsName=" + wsName + ", type=" + type + ", version=" + version
                + "]";
    }
}
