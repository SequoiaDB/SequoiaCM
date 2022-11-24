package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmDirDaoFactory {

    ScmDirDao createScmDirDao(ScmOmSession session);
}
