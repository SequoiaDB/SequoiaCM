package com.sequoiacm.diagnose.datasource;

import org.bson.BSONObject;

import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmServerException;

import java.util.HashMap;
import java.util.Map;

public class ScmWorkspaceInfo {
    private ScmWorkspaceItem currentWorkspace;
    private Map</* versionId */Integer, ScmWorkspaceItem> workspaceHistories = new HashMap<>();

    public ScmWorkspaceInfo(BSONObject workspaceObj, ScmSiteMgr siteMgr) throws ScmServerException {
        currentWorkspace = new ScmWorkspaceItem(workspaceObj, siteMgr);
    }

    public void addHistoryWsItem(ScmWorkspaceItem item) {
        workspaceHistories.put(item.getVersion(), item);
    }

    public ScmLocation getScmLocation(int version, int siteId) {
        if (currentWorkspace.getVersion() == version) {
            return currentWorkspace.getScmLocation(siteId);
        }
        else {
            ScmWorkspaceItem scmWorkspaceItem = workspaceHistories.get(version);
            if (null == scmWorkspaceItem) {
                return null;
            }
            return scmWorkspaceItem.getScmLocation(siteId);
        }
    }

    public String getName() {
        return currentWorkspace.getName();
    }
}
