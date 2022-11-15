package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmPrivilegeService {

    public OmRoleInfo listPrivi(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException;
}
