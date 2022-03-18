package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmMetaDataDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmMetaDataDaoFactory {

    ScmMetaDataDao createMetaDataDao(ScmOmSession session);
}
