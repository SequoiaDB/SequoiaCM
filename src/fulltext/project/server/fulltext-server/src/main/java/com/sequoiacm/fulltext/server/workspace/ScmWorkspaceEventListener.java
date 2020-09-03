package com.sequoiacm.fulltext.server.workspace;

public interface ScmWorkspaceEventListener {
    void onWorkspaceAdd(ScmWorkspaceInfo ws);

    void onWorkspaceRemove(ScmWorkspaceInfo ws);

    void onWorkspaceUpdate(ScmWorkspaceInfo newWs);
}
