package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.dao.impl.ScmMonitorDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmMonitorDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmMonitorDaoFactoryImpl implements ScmMonitorDaoFactory {
    @Override
    public ScmMonitorDao createMonitorDao(ScmOmSession session) {
        return new ScmMonitorDaoImpl(session);
    }
}
