package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.dispatcher.RestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;

public class ScmPoolingRestSessionImpl extends ScmRestSession {

    private static final Logger logger = LoggerFactory.getLogger(ScmPoolingRestSessionImpl.class);
    private String sessionId = "-1";
    private String user = "";

    private ScmPoolingSessionMgrImpl sessionMgr;

    /**
     * @param url
     *            the url specified to construct a network connection.
     * @param requestConfig
     *            request config.
     * @param preferredRegion
     *            preferred region.
     * @param preferredZone
     *            preferred zone.
     * @param sessionMgr
     *            session manager.
     * @throws ScmException
     *             if error happens.
     */
    ScmPoolingRestSessionImpl(String url, ScmRequestConfig requestConfig, String preferredRegion,
            String preferredZone, ScmPoolingSessionMgrImpl sessionMgr) throws ScmException {
        super(url, preferredRegion, preferredZone,
                new RestDispatcher(url, requestConfig, sessionMgr.getHttpClient()));
        this.sessionMgr = sessionMgr;
    }

    /**
     * @param url
     *            the url specified to construct a network connection.
     * @param user
     *            user.
     * @param passwd
     *            password.
     * @param requestConfig
     *            request config.
     * @param preferredRegion
     *            preferred region.
     * @param preferredZone
     *            preferred zone.
     * @param sessionMgr
     *            session manager.
     * @throws ScmException
     *             if error happens.
     */
    ScmPoolingRestSessionImpl(String url, String user, String passwd,
            ScmRequestConfig requestConfig, String preferredRegion, String preferredZone,
            ScmPoolingSessionMgrImpl sessionMgr) throws ScmException {
        super(url, preferredRegion, preferredZone,
                new RestDispatcher(url, requestConfig, sessionMgr.getHttpClient()));
        if (user == null) {
            throw new ScmInvalidArgumentException("user is null");
        }
        if (passwd == null) {
            throw new ScmInvalidArgumentException("passwd is null");
        }
        this.user = user;
        this.sessionMgr = sessionMgr;
        login(user, passwd);
    }

    private void login(String user, String password) throws ScmException {
        this.sessionId = dispatcher.login(user, password);
    }

    @Override
    public void close() {
        try {
            this.sessionMgr.releaseSession(this);
        }
        catch (ScmException e) {
            logger.warn("failed to release session, session=" + this, e);
        }
    }
    
    void destroy(boolean needLogout) throws ScmException {
        if (needLogout && !user.isEmpty()) {
            dispatcher.logout();
        }
        super.close();
        closed = true;
    }

    @Override
    public ScmType.SessionType getType() {
        if (!user.isEmpty()) {
            return ScmType.SessionType.AUTH_SESSION;
        }
        return ScmType.SessionType.NOT_AUTH_SESSION;
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
