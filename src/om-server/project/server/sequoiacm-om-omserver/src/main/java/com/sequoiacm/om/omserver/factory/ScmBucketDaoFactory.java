package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmBucketDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmBucketDaoFactory {

    ScmBucketDao createScmBucketDao(ScmOmSession session);
}
