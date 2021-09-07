package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmScheduleDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmScheduleDaoFactory {

    ScmScheduleDao createScheduleDao(ScmOmSession session);
}
