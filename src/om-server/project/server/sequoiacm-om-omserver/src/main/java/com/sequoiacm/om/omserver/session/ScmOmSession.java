package com.sequoiacm.om.omserver.session;

import com.sequoiacm.client.core.ScmSession;


public abstract class ScmOmSession {
    private long lastAccessTime;
    private String sessionId;
    private String user;

    public ScmOmSession(String user, String sessionId) {
        this.user = user;
        this.sessionId = sessionId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public abstract void close();

    public abstract ScmSession getConnection();

    public abstract void resetServiceEndpoint(String serviceName);

    public String getUser() {
        return user;
    }

    void touch() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    long getLastAccessTime() {
        return this.lastAccessTime;
    }

}
