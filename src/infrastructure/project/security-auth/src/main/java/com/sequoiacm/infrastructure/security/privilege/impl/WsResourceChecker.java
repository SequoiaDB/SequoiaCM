package com.sequoiacm.infrastructure.security.privilege.impl;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;


public class WsResourceChecker implements IResourcePrivChecker {
    Map<String, Integer> wsPrivMap = new HashMap<>();

    WsResourceChecker() {
    }

    @Override
    public void clear() {
        wsPrivMap.clear();
    }

    @Override
    public String getType() {
        return ScmWorkspaceResource.RESOURCE_TYPE;
    }

    @Override
    public boolean addResourcePriv(IResource resource, int privilege) {
        ScmWorkspaceResource wsResource = (ScmWorkspaceResource) resource;
        String wsName = wsResource.getWorkspace();
        Integer v = wsPrivMap.get(wsName);
        if (null != v) {
            wsPrivMap.put(wsName, privilege | v);
        }
        else {
            wsPrivMap.put(wsName, privilege);
        }

        return true;
    }

    @Override
    public int getResourcePriv(IResource resource) {
        String wsName = resource.getWorkspace();

        Integer v = wsPrivMap.get(wsName);
        if (null != v) {
            return v;
        }

        return 0;
    }
}
