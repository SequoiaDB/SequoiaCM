package com.sequoiacm.metasource;

import org.bson.BSONObject;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;

public interface MetaDirAccessor extends MetaAccessor {
    public void updateDirInfo(String id, BSONObject newDirInfo) throws ScmMetasourceException;

    public void delete(String id) throws ScmMetasourceException;

    public long updateVersion() throws SdbMetasourceException;

    public void checkAndAmendVersion() throws ScmMetasourceException;
}
