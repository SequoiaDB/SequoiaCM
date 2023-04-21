package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmQuotaDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmQuotaDaoFactory {

    ScmQuotaDao createQuotaDao(ScmOmSession session);
}
