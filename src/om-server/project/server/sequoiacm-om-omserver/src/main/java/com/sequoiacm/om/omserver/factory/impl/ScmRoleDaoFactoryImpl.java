package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmRoleDao;
import com.sequoiacm.om.omserver.dao.impl.ScmRoleDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmRoleDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmRoleDaoFactoryImpl implements ScmRoleDaoFactory {
    @Override
    public ScmRoleDao createRoleDao(ScmOmSession session) {
        return new ScmRoleDaoImpl(session);
    }
}
