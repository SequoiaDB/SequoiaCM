package com.sequoiacm.om.omserver.session;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.om.omserver.dao.ScmBatchDao;
import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.dao.ScmRoleDao;
import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.dao.ScmUserDao;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.dao.impl.ScmBatchDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmDirDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmFileDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmMonitorDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmRoleDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmSiteDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmUserDaoImpl;
import com.sequoiacm.om.omserver.dao.impl.ScmWorkspaceDaoImpl;

public class ScmOmSessionImpl extends ScmOmSession {
    private ScmSession connection;
    private Map<DaoType, Object> daoMaps = new HashMap<>();

    public ScmOmSessionImpl(ScmSession connection) {
        super(connection.getUser(), connection.getSessionId());
        this.connection = connection;
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public ScmUserDao getUserDao() {
        return getDao(DaoType.USER_DAO);
    }

    public ScmSession getConnection() {
        return connection;
    }

    @Override
    public ScmWorkspaceDao getWorkspaceDao() {
        return getDao(DaoType.WORKSPACE_DAO);
    }

    @Override
    public ScmRoleDao getRoleDao() {
        return getDao(DaoType.ROLE_DAO);
    }

    @Override
    public ScmMonitorDao getMonitorDao() {
        return getDao(DaoType.MONITOR_DAO);
    }

    @SuppressWarnings("unchecked")
    private <T> T getDao(DaoType type) {
        Object dao = daoMaps.get(type);
        if (dao == null) {
            synchronized (this) {
                dao = daoMaps.get(type);
                if (dao == null) {
                    dao = type.newInstance(this);
                    daoMaps.put(type, dao);
                }
            }
        }
        return (T) dao;
    }

    @Override
    public void resetServiceEndpoint(String serviceName) {
        connection.resetSiteName(serviceName.toLowerCase());
    }

    @Override
    public ScmBatchDao getBatchDao() {
        return getDao(DaoType.BATCH_DAO);
    }

    @Override
    public ScmDirDao getDirDao() {
        return getDao(DaoType.DIR_DAO);
    }

    @Override
    public ScmFileDao getFileDao() {
        return getDao(DaoType.FILE_DAO);
    }

    @Override
    public ScmSiteDao getSiteDao() {
        return getDao(DaoType.SITE_DAO);
    }

}

enum DaoType {
    USER_DAO {
        @Override
        public ScmUserDaoImpl newInstance(ScmOmSessionImpl session) {
            return new ScmUserDaoImpl(session);
        }
    },

    WORKSPACE_DAO {
        @Override
        public ScmWorkspaceDaoImpl newInstance(ScmOmSessionImpl session) {
            return new ScmWorkspaceDaoImpl(session);
        }
    },

    ROLE_DAO {
        @Override
        public ScmRoleDaoImpl newInstance(ScmOmSessionImpl session) {
            return new ScmRoleDaoImpl(session);
        }
    },

    MONITOR_DAO {
        @Override
        public ScmMonitorDao newInstance(ScmOmSessionImpl session) {
            return new ScmMonitorDaoImpl(session);
        }
    },

    FILE_DAO {
        @Override
        public ScmFileDao newInstance(ScmOmSessionImpl session) {
            return new ScmFileDaoImpl(session);
        }
    },

    BATCH_DAO {
        @Override
        public ScmBatchDao newInstance(ScmOmSessionImpl session) {
            return new ScmBatchDaoImpl(session);
        }
    },

    DIR_DAO {
        @Override
        public ScmDirDao newInstance(ScmOmSessionImpl session) {
            return new ScmDirDaoImpl(session);
        }
    },

    SITE_DAO {
        @Override
        public Object newInstance(ScmOmSessionImpl session) {
            return new ScmSiteDaoImpl(session);
        }

    };

    public abstract Object newInstance(ScmOmSessionImpl connection);
}
