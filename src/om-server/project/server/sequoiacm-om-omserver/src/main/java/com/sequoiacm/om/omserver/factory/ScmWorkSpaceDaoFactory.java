package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmWorkSpaceDaoFactory {

    ScmWorkspaceDao createWorkspaceDao(ScmOmSession session);
}
