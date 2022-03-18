package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmMetaDataDao;
import com.sequoiacm.om.omserver.dao.impl.ScmMetaDataDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmMetaDataDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmMetaDataDaoFactoryImpl implements ScmMetaDataDaoFactory {

    @Override
    public ScmMetaDataDao createMetaDataDao(ScmOmSession session) {
        return new ScmMetaDataDaoImpl(session);
    }
}
