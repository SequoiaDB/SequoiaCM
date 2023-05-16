package com.sequoiacm.om.omserver.service;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import com.sequoiacm.om.omserver.module.OmDeltaStatistics;
import com.sequoiacm.om.omserver.module.OmFileTrafficStatistics;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceCreateInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

public interface ScmWorkspaceService {
    public OmWorkspaceInfoWithStatistics getWorksapceDetailWithStatistics(ScmOmSession session,
            String wsName) throws ScmInternalException, ScmOmServerException;

    public OmWorkspaceDetail getWorkspaceDetail(ScmOmSession session, String wsName)
            throws ScmInternalException, ScmOmServerException;

    public OmWorkspaceDetail getWorkspaceDetail(ScmOmSession session, String wsName,
            boolean forceFetch) throws ScmInternalException, ScmOmServerException;

    public List<OmWorkspaceBasicInfo> getUserRelatedWsList(ScmOmSession session, BSONObject filter,
            BSONObject orderby, long skip, int limit, Boolean isStrictMode)
            throws ScmInternalException, ScmOmServerException;

    public long getWorkspaceCount(ScmOmSession session, BSONObject condition, Boolean strictMode)
            throws ScmInternalException, ScmOmServerException;

    public void removeWorkspaceCache(String user);

    void updateWorkspace(ScmOmSession session, String wsName, OmWorkspaceInfo wsInfo)
            throws ScmOmServerException, ScmInternalException;

    OmFileTrafficStatistics getWorkspaceTraffic(ScmOmSession session, String workspaceName,
            Long beginTime, Long endTime) throws ScmInternalException, ScmOmServerException;

    OmDeltaStatistics getWorkspaceFileDelta(ScmOmSession session, String workspaceName,
            Long beginTime, Long endTime) throws ScmOmServerException, ScmInternalException;

    List<OmWorkspaceBasicInfo> getCreatePrivilegeWsList(ScmOmSession session)
            throws ScmInternalException, ScmOmServerException;

    List<OmBatchOpResult> createWorkspaces(ScmOmSession session,
            OmWorkspaceCreateInfo workspacesInfo) throws ScmOmServerException, ScmInternalException;

    List<OmBatchOpResult> deleteWorkspaces(ScmOmSession session, List<String> wsNames,
            boolean isForce) throws ScmOmServerException, ScmInternalException;
}
