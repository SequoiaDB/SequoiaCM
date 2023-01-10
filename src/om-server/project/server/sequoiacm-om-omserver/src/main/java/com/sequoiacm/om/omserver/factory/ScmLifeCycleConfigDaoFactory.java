package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmLifeCycleConfigDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmLifeCycleConfigDaoFactory {

    ScmLifeCycleConfigDao createLifeCycleConfigDao(ScmOmSession session);
}
