package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.om.omserver.dao.ScmConfigPropsDao;
import com.sequoiacm.om.omserver.dao.impl.ScmConfigPropsDaoImpl;
import com.sequoiacm.om.omserver.factory.ScmConfigPropsDaoFactory;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.stereotype.Component;

@Component
public class ScmPropertiesDaoFactoryImpl implements ScmConfigPropsDaoFactory {

    @Override
    public ScmConfigPropsDao createPropertiesDao(ScmOmSession session) {
        return new ScmConfigPropsDaoImpl(session);
    }
}
