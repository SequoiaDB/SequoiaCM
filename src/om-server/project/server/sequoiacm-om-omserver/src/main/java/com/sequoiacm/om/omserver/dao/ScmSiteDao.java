package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import org.bson.BSONObject;

public interface ScmSiteDao {

    public List<OmSiteInfo> listSite(BSONObject filter, long skip, long limit)
            throws ScmOmServerException, ScmInternalException;

    public long getSiteCount(BSONObject filter) throws ScmOmServerException, ScmInternalException;

    public String getSiteStrategy() throws ScmInternalException;
}
