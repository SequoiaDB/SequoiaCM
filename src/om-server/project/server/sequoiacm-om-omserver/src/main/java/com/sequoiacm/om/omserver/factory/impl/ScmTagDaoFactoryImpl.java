package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmTagDao;
import com.sequoiacm.om.omserver.dao.impl.ScmTagDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmTagDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmTagDaoFactoryImpl implements ScmTagDaoFactory {

    @Override
    public ScmTagDao createTadDao(ScmOmSession session) {
        return new ScmTagDaoImpl(session);
    }
}
