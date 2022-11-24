package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmBatchDao;
import com.sequoiacm.om.omserver.dao.impl.ScmBatchDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmBatchDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmBatchDaoFactoryImpl implements ScmBatchDaoFactory {

    @Override
    public ScmBatchDao createScmBatchDao(ScmOmSession session) {
        return new ScmBatchDaoImpl(session);
    }
}
