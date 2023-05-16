package com.sequoiacm.common;

public enum ScmWorkspaceTagRetrievalStatus {
    DISABLED(CommonDefine.WorkspaceTagRetrievalStatus.DISABLED),
    ENABLED(CommonDefine.WorkspaceTagRetrievalStatus.ENABLED),
    INDEXING(CommonDefine.WorkspaceTagRetrievalStatus.INDEXING),

    UNKNOWN("unknown");

    private String value;

    ScmWorkspaceTagRetrievalStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ScmWorkspaceTagRetrievalStatus fromValue(String value) {
        for (ScmWorkspaceTagRetrievalStatus status : ScmWorkspaceTagRetrievalStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
