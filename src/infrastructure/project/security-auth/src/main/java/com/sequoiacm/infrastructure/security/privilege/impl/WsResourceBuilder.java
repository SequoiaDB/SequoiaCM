package com.sequoiacm.infrastructure.security.privilege.impl;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;

public class WsResourceBuilder implements IResourceBuilder {

    @Override
    public IResourcePrivChecker createResourceChecker() {
        return new WsResourceChecker();
    }

    @Override
    public String getResourceType() {
        return ScmWorkspaceResource.RESOURCE_TYPE;
    }

    @Override
    public String toStringFormat(IResource resource) {
        ScmWorkspaceResource wsResource = (ScmWorkspaceResource) resource;
        return wsResource.toStringFormat();
    }

    @Override
    public IResource fromStringFormat(String resource) {
        return new ScmWorkspaceResource(resource);
    }

    @Override
    public IResource fromStringFormat(String resource, boolean isNeedFormat) {
        return fromStringFormat(resource);
    }
}
