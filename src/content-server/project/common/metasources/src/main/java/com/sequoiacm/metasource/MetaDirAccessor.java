package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaDirAccessor extends MetaAccessor {
    public void updateDirInfo(String id, BSONObject newDirInfo) throws ScmMetasourceException;

    public void delete(String id) throws ScmMetasourceException;
}
