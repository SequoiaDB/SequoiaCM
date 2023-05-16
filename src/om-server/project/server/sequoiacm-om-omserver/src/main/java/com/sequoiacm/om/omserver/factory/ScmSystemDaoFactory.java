package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmSystemDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmSystemDaoFactory {

    ScmSystemDao createSystemDao(ScmOmSession session);
}
