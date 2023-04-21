package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmQuotaDao;
import com.sequoiacm.om.omserver.dao.impl.ScmQuotaDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmQuotaDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmQuotaDaoFactoryImpl implements ScmQuotaDaoFactory {
    @Override
    public ScmQuotaDao createQuotaDao(ScmOmSession session) {
        return new ScmQuotaDaoImpl(session);
    }
}
