package com.sequoiacm.infrastructure.config.core.msg.workspace;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class WorkspaceFilter implements ConfigFilter {

    private String wsName;
    private String tagRetrievalStatus;

    public WorkspaceFilter() {
    }

    public WorkspaceFilter(String wsName) {
        this.wsName = wsName;
    }

    public String getWsName() {
        return wsName;
    }

    public void setTagRetrievalStatus(String tagRetrievalStatus) {
        this.tagRetrievalStatus = tagRetrievalStatus;
    }

    public String getTagRetrievalStatus() {
        return tagRetrievalStatus;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (wsName != null) {
            obj.put(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME, wsName);
        }
        if(tagRetrievalStatus != null) {
            obj.put(ScmRestArgDefine.WORKSPACE_CONF_TAG_RETRIEVAL_STATUS, tagRetrievalStatus);
        }
        return obj;

    }
}
