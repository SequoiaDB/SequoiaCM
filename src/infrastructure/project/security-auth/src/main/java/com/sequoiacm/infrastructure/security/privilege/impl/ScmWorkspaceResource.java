package com.sequoiacm.infrastructure.security.privilege.impl;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.ScmResourceTypeDefine;

public class ScmWorkspaceResource implements IResource {

    public static final String RESOURCE_TYPE = ScmResourceTypeDefine.TYPE_WS;
    private String wsName;

    public ScmWorkspaceResource(String resource) {
        wsName = resource;
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getWorkspace() {
        return wsName;
    }

    @Override
    public String toStringFormat() {
        return wsName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RESOURCE_TYPE).append(":").append(wsName);
        return sb.toString();
    }

}
