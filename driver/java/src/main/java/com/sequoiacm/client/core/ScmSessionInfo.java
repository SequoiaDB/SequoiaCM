package com.sequoiacm.client.core;

/**
 * SCM Session Information
 */
public interface ScmSessionInfo {

    /**
     * Get session id.
     * @return session id
     */
    String getSessionId();

    /**
     * Get username of the session.
     * @return username
     */
    String getUsername();

    /**
     * Get creation time of the session.
     * @return creation time in milliseconds
     */
    long getCreationTime();

    /**
     * Get last accessed time of the session.
     * @return last accessed time in milliseconds
     */
    long getLastAccessedTime();

    /**
     * Get max inactive interval of the session.
     * @return max inactive interval in seconds
     */
    int getMaxInactiveInterval();
}
