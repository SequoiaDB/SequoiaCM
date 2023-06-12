package com.sequoiacm.infrastructure.config.core.msg.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;

import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.WORKSPACE)
public class WorkspaceNotifyOption implements NotifyOption {

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME)
    private String workspaceName;

    @JsonProperty(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACEVERSION)
    private Integer version;

    public WorkspaceNotifyOption(String wsName, Integer version) {
        this.workspaceName = wsName;
        this.version = version;
    }

    public WorkspaceNotifyOption() {
    }

    public Integer getVersion() {
        return version;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    @Override
    public String getBusinessName() {
        return workspaceName;
    }

    @Override
    public Version getBusinessVersion() {
        return new Version(ScmBusinessTypeDefine.WORKSPACE, workspaceName, version);
    }


    @Override
    public String toString() {
        return "WorkspaceNotifyOption{" + "workspaceName='" + workspaceName + '\'' + ", version="
                + version + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkspaceNotifyOption that = (WorkspaceNotifyOption) o;
        return Objects.equals(workspaceName, that.workspaceName) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceName, version);
    }
}
