package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmTagDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmTagDaoFactory {

    ScmTagDao createTadDao(ScmOmSession session);
}
