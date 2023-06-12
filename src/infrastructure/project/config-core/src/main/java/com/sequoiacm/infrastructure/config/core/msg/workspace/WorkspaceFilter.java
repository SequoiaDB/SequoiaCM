package com.sequoiacm.infrastructure.config.core.msg.workspace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.WORKSPACE)
public class WorkspaceFilter implements ConfigFilter {

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME)
    private String wsName;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_TAG_RETRIEVAL_STATUS)
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkspaceFilter that = (WorkspaceFilter) o;
        return Objects.equals(wsName, that.wsName) && Objects.equals(tagRetrievalStatus, that.tagRetrievalStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, tagRetrievalStatus);
    }
}
