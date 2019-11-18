package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaAttrAccessor extends MetaAccessor{

    void delete(String attrId) throws ScmMetasourceException;

    boolean update(String attrId, BSONObject newAttrInfo) 
            throws ScmMetasourceException;
}

