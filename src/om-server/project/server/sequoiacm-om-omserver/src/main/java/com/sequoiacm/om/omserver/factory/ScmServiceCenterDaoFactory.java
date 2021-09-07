package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmServiceCenterDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmServiceCenterDaoFactory {

    ScmServiceCenterDao createServiceCenterDao(ScmOmSession session);
}
