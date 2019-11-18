package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;

public interface ScmSiteDao {
    public List<OmSiteInfo> listSite() throws ScmOmServerException, ScmInternalException;
}
