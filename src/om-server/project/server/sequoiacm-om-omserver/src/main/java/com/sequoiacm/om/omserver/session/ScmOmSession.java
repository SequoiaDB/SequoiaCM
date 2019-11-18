package com.sequoiacm.om.omserver.session;

import com.sequoiacm.om.omserver.dao.ScmBatchDao;
import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.dao.ScmRoleDao;
import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.dao.ScmUserDao;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;

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

    public abstract ScmUserDao getUserDao();

    public abstract ScmWorkspaceDao getWorkspaceDao();

    public abstract ScmRoleDao getRoleDao();

    public abstract ScmMonitorDao getMonitorDao();

    public abstract ScmBatchDao getBatchDao();

    public abstract ScmDirDao getDirDao();

    public abstract ScmFileDao getFileDao();

    public abstract ScmSiteDao getSiteDao();
}
