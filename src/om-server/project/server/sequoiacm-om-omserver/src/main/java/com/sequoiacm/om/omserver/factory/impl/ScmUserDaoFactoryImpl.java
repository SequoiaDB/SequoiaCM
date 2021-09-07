package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmUserDao;
import com.sequoiacm.om.omserver.dao.impl.ScmUserDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmUserDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmUserDaoFactoryImpl implements ScmUserDaoFactory {

    @Override
    public ScmUserDao createUserDao(ScmOmSession session) {
        return new ScmUserDaoImpl(session);
    }
}
