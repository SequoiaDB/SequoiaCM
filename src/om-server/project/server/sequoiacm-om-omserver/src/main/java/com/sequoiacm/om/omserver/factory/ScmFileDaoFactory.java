package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmFileDaoFactory {

    ScmFileDao  createFileDao(ScmOmSession session);
}
