package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmDirectoryInfoWithSubDir;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.List;

public interface ScmDirService {

    List<OmDirectoryInfoWithSubDir> listSubDir(ScmOmSession session, String wsName, String dirId,
            BSONObject orderBy, int skip, int limit)
            throws ScmOmServerException, ScmInternalException;
}
