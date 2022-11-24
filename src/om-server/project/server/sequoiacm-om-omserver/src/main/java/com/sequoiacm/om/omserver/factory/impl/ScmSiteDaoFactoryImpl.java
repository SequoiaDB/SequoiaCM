package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.dao.impl.ScmSiteDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmSiteDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmSiteDaoFactoryImpl implements ScmSiteDaoFactory {

    @Override
    public ScmSiteDao createSiteDao(ScmOmSession session) {
        return new ScmSiteDaoImpl(session);
    }
}
