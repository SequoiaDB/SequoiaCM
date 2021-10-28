package com.sequoiacm.om.omserver.service;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

public interface ScmWorkspaceService {
    public OmWorkspaceInfoWithStatistics getWorksapceDetailWithStatistics(ScmOmSession session,
            String workspaceName) throws ScmInternalException, ScmOmServerException;

    public OmWorkspaceDetail getWorkspaceDetail(ScmOmSession session, String workspaceName)
            throws ScmInternalException, ScmOmServerException;

    public List<OmWorkspaceBasicInfo> getUserRelatedWsList(ScmOmSession session, BSONObject filter,
            BSONObject orderby, long skip, int limit)
            throws ScmInternalException, ScmOmServerException;

    public long getWorkspaceCount(ScmOmSession session, BSONObject condition)
            throws ScmInternalException, ScmOmServerException;

    public void removeWorkspaceCache(String user);
}
