package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.dao.ScmConfigPropsDao;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmConfigPropsDaoFactory {

    ScmConfigPropsDao createPropertiesDao(ScmOmSession session);
}
