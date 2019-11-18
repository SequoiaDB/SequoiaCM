package com.sequoiacm.metasource;

public interface MetaWorkspaceAccessor extends MetaAccessor {
    public void delete(String wsName) throws ScmMetasourceException;
}
