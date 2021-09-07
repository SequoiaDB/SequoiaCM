package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import java.util.List;

public interface ScmSiteService {
    public String getSiteById(ScmOmSession session, int id)
            throws ScmInternalException, ScmOmServerException;

    public List<OmSiteInfo> getSiteList(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException;

    public String getSiteStrategy(ScmOmSession session) throws ScmInternalException;
}
