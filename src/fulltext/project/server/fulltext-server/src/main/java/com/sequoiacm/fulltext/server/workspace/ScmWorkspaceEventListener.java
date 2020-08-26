package com.sequoiacm.fulltext.server.workspace;

public interface ScmWorkspaceEventListener {
    void onWorkspaceAdd(ScmWorkspaceInfo ws);

    void onWorkspaceRemove(String ws);

    void onWorkspaceUpdate(ScmWorkspaceInfo newWs);
}
