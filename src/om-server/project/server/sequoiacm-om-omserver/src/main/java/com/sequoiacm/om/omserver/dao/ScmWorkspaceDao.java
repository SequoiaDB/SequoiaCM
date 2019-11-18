package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;

public interface ScmWorkspaceDao {
    public List<OmWorkspaceBasicInfo> getWorkspaceList(long skip, int limit)
            throws ScmInternalException, ScmOmServerException;

    public OmWorkspaceDetail getWorkspaceDetail(String wsName)
            throws ScmInternalException, ScmOmServerException;
}
