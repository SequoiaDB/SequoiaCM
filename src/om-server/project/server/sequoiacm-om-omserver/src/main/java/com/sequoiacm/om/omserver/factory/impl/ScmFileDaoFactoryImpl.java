package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.dao.impl.ScmFileDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmFileDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmFileDaoFactoryImpl implements ScmFileDaoFactory {
    @Override
    public ScmFileDao createFileDao(ScmOmSession session) {
        return new ScmFileDaoImpl(session);
    }
}
