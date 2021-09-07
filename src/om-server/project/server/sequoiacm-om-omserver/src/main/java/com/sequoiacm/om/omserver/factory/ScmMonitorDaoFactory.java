package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmMonitorDaoFactory {

    ScmMonitorDao createMonitorDao(ScmOmSession session);
}
