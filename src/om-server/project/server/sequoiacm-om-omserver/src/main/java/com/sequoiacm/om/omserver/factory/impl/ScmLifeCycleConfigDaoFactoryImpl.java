package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmLifeCycleConfigDao;
import com.sequoiacm.om.omserver.dao.impl.ScmLifeCycleConfigDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmLifeCycleConfigDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmLifeCycleConfigDaoFactoryImpl implements ScmLifeCycleConfigDaoFactory {

    @Override
    public ScmLifeCycleConfigDao createLifeCycleConfigDao(ScmOmSession session) {
        return new ScmLifeCycleConfigDaoImpl(session);
    }
}
