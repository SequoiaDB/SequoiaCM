package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmTaskDao;
import com.sequoiacm.om.omserver.dao.impl.ScmTaskDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmTaskDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmTaskDaoFactoryImpl implements ScmTaskDaoFactory {

    @Override
    public ScmTaskDao createTaskDao(ScmOmSession session) {
        return new ScmTaskDaoImpl(session);
    }
}
