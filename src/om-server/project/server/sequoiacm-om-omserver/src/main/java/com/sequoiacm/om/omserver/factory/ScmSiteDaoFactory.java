package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmSiteDaoFactory {

    ScmSiteDao createSiteDao(ScmOmSession session);
}
