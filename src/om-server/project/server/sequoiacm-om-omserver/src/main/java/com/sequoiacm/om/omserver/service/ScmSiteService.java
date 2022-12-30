package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.List;
import java.util.Map;

public interface ScmSiteService {
    public String getSiteById(ScmOmSession session, int id)
            throws ScmInternalException, ScmOmServerException;

    public List<OmSiteInfo> getSiteList(ScmOmSession session, BSONObject filter, long skip,
            long limit) throws ScmOmServerException, ScmInternalException;

    public long getSiteCount(ScmOmSession session, BSONObject filter)
            throws ScmOmServerException, ScmInternalException;

    public String getSiteStrategy(ScmOmSession session) throws ScmInternalException;

    Map<String, OmSiteInfo> getSitesAsMap(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException;
}
