package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.dao.impl.ScmWorkspaceDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmWorkSpaceDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmWorkspaceDaoFactoryImpl implements ScmWorkSpaceDaoFactory {
    @Override
    public ScmWorkspaceDao createWorkspaceDao(ScmOmSession session) {
        return new ScmWorkspaceDaoImpl(session);
    }
}
