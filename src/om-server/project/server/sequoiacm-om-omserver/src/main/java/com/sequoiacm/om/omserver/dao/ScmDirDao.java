package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmDirectoryBasic;
import com.sequoiacm.om.omserver.module.OmDirectoryInfoWithSubDir;
import org.bson.BSONObject;

import java.util.List;

public interface ScmDirDao {

    long countDir(String wsName, BSONObject condition) throws ScmInternalException;

    List<OmDirectoryBasic> getDirList(String wsName, BSONObject condition, BSONObject orderBy,
            int skip, int limit) throws ScmInternalException;

    List<OmDirectoryInfoWithSubDir> listSubDir(String wsName, String dirId, BSONObject orderBy,
            int skip, int limit) throws ScmInternalException;
}
