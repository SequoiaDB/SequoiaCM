package com.sequoiacm.infrastructure.security.privilege.impl;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;
import com.sequoiacm.infrastructrue.security.privilege.ScmResourceTypeDefine;

public class WsAllResourceBuilder implements IResourceBuilder {

    @Override
    public IResourcePrivChecker createResourceChecker() {
        return new WsAllResourceChecker();
    }

    @Override
    public String toStringFormat(IResource resource) {
        return resource.toStringFormat();
    }

    @Override
    public IResource fromStringFormat(String resource) {
        return new ScmWsAllResource();
    }

    @Override
    public String getResourceType() {
        return ScmResourceTypeDefine.TYPE_WS_ALL;
    }

}
