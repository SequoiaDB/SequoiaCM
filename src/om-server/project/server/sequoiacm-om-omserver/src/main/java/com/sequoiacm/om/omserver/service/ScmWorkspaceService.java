package com.sequoiacm.om.omserver.service;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmWorkspaceService {
    public OmWorkspaceInfoWithStatistics getWorksapceDetailWithStatistics(ScmOmSession session,
            String workspaceName) throws ScmInternalException, ScmOmServerException;

    public OmWorkspaceDetail getWorksapceDetail(ScmOmSession session, String workspaceName)
            throws ScmInternalException, ScmOmServerException;

    public List<OmWorkspaceBasicInfo> getWorkspaceList(ScmOmSession session, long skip, int limit)
            throws ScmInternalException, ScmOmServerException;
}
