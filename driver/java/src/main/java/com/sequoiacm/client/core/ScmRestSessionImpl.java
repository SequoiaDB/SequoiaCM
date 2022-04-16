package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.ScmHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmRestSessionImpl extends ScmRestSession {

    private static final Logger logger = LoggerFactory.getLogger(ScmRestSessionImpl.class);
    private String sessionId = "-1";
    private String user = "";

    /**
     * @param url
     *            the url specified to construct a network connection.
     * @param requestConfig
     *            request config.
     * @param preferredRegion
     *            preferred region.
     * @param preferredZone
     *            preferred zone.
     * @throws ScmException
     *             if error happens.
     */
    ScmRestSessionImpl(String url, ScmRequestConfig requestConfig, String preferredRegion,
            String preferredZone) throws ScmException {
        super(url, requestConfig, preferredRegion, preferredZone);
        checkHealth(url);
    }

    private void checkHealth(String url) throws ScmSystemException {
        boolean isHealth = ScmHelper.checkGatewayHealth(url, dispatcher);
        if (!isHealth) {
            throw new ScmSystemException("node is unhealthy: url=" + url);
        }
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
     * @throws ScmException
     *             if error happens.
     */
    ScmRestSessionImpl(String url, String user, String passwd, ScmRequestConfig requestConfig,
            String preferredRegion, String preferredZone) throws ScmException {
        super(url, requestConfig, preferredRegion, preferredZone);
        if (user == null) {
            throw new ScmInvalidArgumentException("user is null");
        }
        if (passwd == null) {
            throw new ScmInvalidArgumentException("passwd is null");
        }
        this.user = user;
        login(user, passwd);

    }

    private void login(String user, String password) throws ScmException {
        this.sessionId = dispatcher.login(user, password);
    }

    private void logout() {
        try {
            dispatcher.logout();
        }
        catch (Exception e) {
            logger.debug("session logout failed:sessionId={}", sessionId, e);
        }
    }

    @Override
    public void close() {
        if (!user.isEmpty()) {
            logout();
        }
        super.close();
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
