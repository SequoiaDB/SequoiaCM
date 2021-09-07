package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.dao.impl.ScmDirDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmDirDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmDirDaoFactoryImpl implements ScmDirDaoFactory {
    @Override
    public ScmDirDao createScmDirDao(ScmOmSession session) {
        return new ScmDirDaoImpl(session);
    }
}
