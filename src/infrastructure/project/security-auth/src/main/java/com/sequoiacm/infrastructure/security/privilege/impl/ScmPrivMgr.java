package com.sequoiacm.infrastructure.security.privilege.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;


public class ScmPrivMgr {
    private int version = -1;
    private Map<String, ScmUserPriv> userPrivilegeMap = new ConcurrentHashMap<>();

    public ScmPrivMgr() {
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public boolean check(String user, IResource resource, int op) {
        ScmUserPriv c = userPrivilegeMap.get(user);
        if (null != c) {
            return c.check(resource, op);
        }

        return false;
    }

    public boolean addPrivilege(String user, IResource resource, int priv, IResourceBuilder builder) {
        if (null == builder) {
            // if specified resource's factory is not exist, do not add it.
            return false;
        }

        ScmUserPriv c = userPrivilegeMap.get(user);
        if (null == c) {
            c = new ScmUserPriv(user);
            userPrivilegeMap.put(user, c);
        }

        return c.addResource(resource, priv, builder);
    }

    public void clear() {
        userPrivilegeMap.clear();
    }

    public boolean hasAllPriority(String user, ScmWorkspaceResource wsResource) {
        ScmUserPriv c = userPrivilegeMap.get(user);
        if (null != c) {
            return c.hasAllPriority(wsResource);
        }

        return false;
    }
}
