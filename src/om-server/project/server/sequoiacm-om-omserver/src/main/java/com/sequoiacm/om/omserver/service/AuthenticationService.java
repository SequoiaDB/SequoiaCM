package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface AuthenticationService {
    public ScmOmSession login(String username, String password) throws ScmInternalException;
    public void logout(ScmOmSession session) throws ScmOmServerException;
}
