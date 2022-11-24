package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmUserDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmUserDaoFactory {

    ScmUserDao createUserDao(ScmOmSession session);
}
