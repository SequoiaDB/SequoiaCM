package com.sequoiacm.client.core;

import java.io.Closeable;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.exception.ScmException;

/**
 * Session manager.
 */
public interface ScmSessionMgr extends Closeable {
    /**
     * Gets a session from the session manager.
     *
     * @param type
     *            session type.
     * @return ScmSession.
     * @throws ScmException
     *             if error happens.
     */
    ScmSession getSession(SessionType type) throws ScmException;

    /**
     * Gets a session of type AUTH_SESSION from the session manager.
     *
     * @return ScmSession.
     * @throws ScmException
     *             if error happens.
     */
    ScmSession getSession() throws ScmException;

    /**
     * Close the session manager.
     */
    @Override
    void close();
}