package com.sequoiacm.om.omserver.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.AuthenticationService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import com.sequoiacm.om.omserver.session.ScmOmSessionFactory;
import com.sequoiacm.om.omserver.session.ScmOmSessionMgr;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private ScmOmSessionMgr sessionMgr;

    @Autowired
    private ScmOmSessionFactory sessionFactory;

    @Override
    public ScmOmSession login(String username, String password) throws ScmInternalException {
        ScmOmSession session = sessionFactory.createSession(username, password);
        sessionMgr.saveSession(session);
        return session;
    }

    @Override
    public void logout(ScmOmSession session) throws ScmOmServerException {
        sessionMgr.deleteSession(session);
    }

}
