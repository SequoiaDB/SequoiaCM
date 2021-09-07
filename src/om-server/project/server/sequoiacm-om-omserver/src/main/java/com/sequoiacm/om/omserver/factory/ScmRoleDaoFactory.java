package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmRoleDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmRoleDaoFactory {

    ScmRoleDao createRoleDao(ScmOmSession session);
}
