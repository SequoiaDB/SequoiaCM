package com.sequoiacm.infrastructure.security.privilege.impl;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.ScmResourceTypeDefine;

public class ScmWsAllResource implements IResource {
    public static final String TYPE = ScmResourceTypeDefine.TYPE_WS_ALL;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getWorkspace() {
        return TYPE;
    }

    @Override
    public String toStringFormat() {
        return TYPE;
    }

}
