package com.sequoiacm.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.dispatcher.MessageDispatcher;
import com.sequoiacm.client.dispatcher.RestDispatcher;
import com.sequoiacm.client.exception.ScmException;

abstract class ScmRestSession extends ScmSession {
    private static final Logger logger = LoggerFactory.getLogger(ScmRestSession.class);
    private final String preferredRegion;
    private final String preferredZone;

    protected MessageDispatcher dispatcher;
    private String url;
    private boolean closed;

    /**
     *
     * @param url
     *            the url specified to construct a network connection
     * @throws ScmException
     *             if the user or the passwd is incorrect
     *
     */
    ScmRestSession(String url, ScmRequestConfig requestConfig, String preferredRegion,
            String preferredZone) throws ScmException {
        this.url = url;
        this.dispatcher = new RestDispatcher(url, requestConfig);
        this.closed = false;
        this.preferredRegion = preferredRegion;
        this.preferredZone = preferredZone;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            dispatcher.close();
        }
        catch (Exception e) {
            logger.warn("close dispatcher failed", e);
        }
        closed = true;
    }

    @Override
    MessageDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public abstract String getUser();

    @Override
    public abstract String getSessionId();

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "url=" + getUrl() + ",sessionId=" + getSessionId();
    }

    @Override
    public void resetSiteName(String siteName) {
        dispatcher.resetRemainUrl(siteName);
    }

    @Override
    public String getPreferredRegion() {
        return preferredRegion;
    }

    @Override
    public String getPreferredZone() {
        return preferredZone;
    }
}
