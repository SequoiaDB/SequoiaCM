package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaClassAccessor extends MetaAccessor{
    
    void delete(String classId) throws ScmMetasourceException;

    boolean update(String classId, BSONObject newClassInfo) 
            throws ScmMetasourceException;

}
