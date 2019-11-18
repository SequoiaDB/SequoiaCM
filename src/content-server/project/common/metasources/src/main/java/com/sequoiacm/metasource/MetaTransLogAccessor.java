package com.sequoiacm.metasource;

public interface MetaTransLogAccessor extends MetaAccessor {
    public void delete(String transId) throws ScmMetasourceException;
}
