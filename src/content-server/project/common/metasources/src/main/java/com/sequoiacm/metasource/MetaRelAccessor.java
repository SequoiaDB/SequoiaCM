package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaRelAccessor extends MetaAccessor {

    public void updateRel(String fileId, String parentDirId, String fileName, BSONObject newInfo)
            throws ScmMetasourceException;

    public void deleteRel(String fileId, String parentDirId, String fileName)
            throws ScmMetasourceException;
}
