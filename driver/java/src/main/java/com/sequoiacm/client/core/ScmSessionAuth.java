package com.sequoiacm.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;

class ScmSessionAuth extends ScmRestSession {
    private static final Logger logger = LoggerFactory.getLogger(ScmSessionAuth.class);
    private String sessionId = "-1";
    private String user;

    /**
     *
     * @param url
     *            the url specified to construct a network connection
     * @param user
     *            username
     * @param passwd
     *            password
     * @throws ScmException
     *             if the user or the passwd is incorrect
     *
     */
    public ScmSessionAuth(String url, String user, String passwd, ScmRequestConfig requestConfig)
            throws ScmException {
        super(url, requestConfig);
        if (user == null) {
            throw new ScmInvalidArgumentException("user is null");
        }
        if (passwd == null) {
            throw new ScmInvalidArgumentException("passwd is null");
        }
        this.user = user;
        login(user, passwd);
    }

    @Override
    public void close() {
        logout();
        super.close();
    }

    private void login(String user, String password) throws ScmException {
        this.sessionId = dispatcher.login(user, password);
    }

    private void logout() {
        try {
            dispatcher.logout();
        }
        catch (Exception e) {
            logger.warn("session logout failed:sessionId={}", sessionId, e);
        }
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }
}
