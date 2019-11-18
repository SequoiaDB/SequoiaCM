package com.sequoiacm.infrastructure.config.core.msg.workspace;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class WorkspaceNotifyOption implements NotifyOption {
    private String workspaceName;
    private Integer version;
    private EventType eventType;

    public WorkspaceNotifyOption(String wsName, Integer version, EventType type) {
        this.workspaceName = wsName;
        this.version = version;
        this.eventType = type;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    @Override
    public DefaultVersion getVersion() {
        if (eventType == EventType.DELTE) {
            return new DefaultVersion(ScmConfigNameDefine.WORKSPACE, workspaceName, -1);
        }
        return new DefaultVersion(ScmConfigNameDefine.WORKSPACE, workspaceName, version);
    }

    @Override
    public String toString() {
        return "workspaceName=" + workspaceName + ",eventType=" + eventType + ",version=" + version;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME, workspaceName);
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACEVERSION, version);
        return obj;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

}
