package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmScheduleDao;
import com.sequoiacm.om.omserver.dao.impl.ScmScheduleDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmScheduleDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;


@Component
public class ScmScheduleDaoFactoryImpl implements ScmScheduleDaoFactory {

    @Override
    public ScmScheduleDao createScheduleDao(ScmOmSession session) {
        return new ScmScheduleDaoImpl(session);
    }
}
