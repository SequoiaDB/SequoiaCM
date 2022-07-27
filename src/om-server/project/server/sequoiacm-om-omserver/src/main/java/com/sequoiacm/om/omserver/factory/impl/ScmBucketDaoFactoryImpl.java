package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmBucketDao;
import com.sequoiacm.om.omserver.dao.impl.ScmBucketDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmBucketDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmBucketDaoFactoryImpl implements ScmBucketDaoFactory {

    @Override
    public ScmBucketDao createScmBucketDao(ScmOmSession session) {
        return new ScmBucketDaoImpl(session);
    }
}
