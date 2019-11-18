package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmSiteService {
    public String getSiteById(ScmOmSession session, int id)
            throws ScmInternalException, ScmOmServerException;
}
