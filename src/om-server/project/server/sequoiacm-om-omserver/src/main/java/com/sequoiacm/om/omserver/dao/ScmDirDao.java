package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.om.omserver.exception.ScmInternalException;

public interface ScmDirDao {
    public long countDir(String wsName) throws ScmInternalException;
}
