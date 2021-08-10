package com.sequoiacm.client.core;

import com.sequoiacm.client.dispatcher.MessageDispatcher;

import java.io.Closeable;

/**
 * SCM Session
 */
public abstract class ScmSession implements Closeable {

    /**
     * Get user of the session.
     *
     * @return username
     */
    public abstract String getUser();

    /**
     * Get session id of the session.
     *
     * @return session id
     */
    public abstract String getSessionId();

    /**
     * Get url of the session.
     *
     * @return url
     */
    public abstract String getUrl();

    @Override
    public abstract void close();

    /**
     * Check if the session closed.
     *
     * @return true if the session is closed, otherwise false
     */
    public abstract boolean isClosed();

    public abstract void resetSiteName(String siteName);

    abstract MessageDispatcher getDispatcher();

    public abstract String getPreferredRegion();

    public abstract String getPreferredZone();
}
