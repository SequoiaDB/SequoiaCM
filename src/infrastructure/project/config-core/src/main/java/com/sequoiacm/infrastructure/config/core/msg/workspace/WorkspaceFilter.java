package com.sequoiacm.infrastructure.config.core.msg.workspace;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class WorkspaceFilter implements ConfigFilter {

    private String wsName;

    public WorkspaceFilter(String wsName) {
        this.wsName = wsName;
    }

    public String getWsName() {
        return wsName;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (wsName != null) {
            obj.put(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME, wsName);
        }
        return obj;

    }
}
