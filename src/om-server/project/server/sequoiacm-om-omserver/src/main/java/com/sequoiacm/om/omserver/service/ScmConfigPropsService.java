package com.sequoiacm.om.omserver.service;

import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmConfPropsParam;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmConfigPropsService {

    ScmUpdateConfResultSet updateProperties(ScmOmSession session, OmConfPropsParam config)
            throws ScmOmServerException, ScmInternalException;
}
