package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmServiceCenterDao;
import com.sequoiacm.om.omserver.dao.impl.ScmServiceCenterDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmServiceCenterDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmServiceCenterDaoFactoryImpl implements ScmServiceCenterDaoFactory {
    @Override
    public ScmServiceCenterDao createServiceCenterDao(ScmOmSession session) {
        return new ScmServiceCenterDaoImpl(session);
    }
}
