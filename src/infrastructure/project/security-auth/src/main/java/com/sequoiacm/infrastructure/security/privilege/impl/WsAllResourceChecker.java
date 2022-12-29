package com.sequoiacm.infrastructure.security.privilege.impl;

import org.springframework.util.Assert;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;
import com.sequoiacm.infrastructrue.security.privilege.ScmResourceTypeDefine;

public class WsAllResourceChecker implements IResourcePrivChecker {
    private int priv = 0;

    @Override
    public void clear() {
        priv = 0;
    }

    @Override
    public boolean addResourcePriv(IResource resource, int priv) {
        Assert.isTrue(resource.getType().equals(ScmResourceTypeDefine.TYPE_WS_ALL), "resource type missmatch");
        this.priv = this.priv | priv;
        return true;
    }

    @Override
    public String getType() {
        return ScmResourceTypeDefine.TYPE_WS_ALL;
    }

    @Override
    public int getResourcePriv(IResource resource) {
        return priv;
    }

}
