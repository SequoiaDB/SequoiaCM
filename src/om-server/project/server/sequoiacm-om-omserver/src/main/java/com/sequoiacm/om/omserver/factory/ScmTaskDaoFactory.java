package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmTaskDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmTaskDaoFactory {

    ScmTaskDao createTaskDao(ScmOmSession session);
}
