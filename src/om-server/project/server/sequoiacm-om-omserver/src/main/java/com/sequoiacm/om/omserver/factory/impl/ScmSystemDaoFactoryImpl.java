package com.sequoiacm.om.omserver.factory.impl;

import org.springframework.stereotype.Component;

import com.sequoiacm.om.omserver.dao.ScmSystemDao;
import com.sequoiacm.om.omserver.dao.impl.ScmSystemDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmSystemDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Component
public class ScmSystemDaoFactoryImpl implements ScmSystemDaoFactory {

    @Override
    public ScmSystemDao createSystemDao(ScmOmSession session) {
        return new ScmSystemDaoImpl(session);
    }
}
