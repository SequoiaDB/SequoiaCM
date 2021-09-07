package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmBatchDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmBatchDaoFactory {

    ScmBatchDao createScmBatchDao(ScmOmSession session);
}
