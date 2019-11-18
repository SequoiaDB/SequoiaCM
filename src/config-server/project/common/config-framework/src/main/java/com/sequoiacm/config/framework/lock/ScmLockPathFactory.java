package com.sequoiacm.config.framework.lock;

public class ScmLockPathFactory {

    public static ScmLockPath createWorkspaceConfOpLockPath() {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES_CONF_OP_MUTEX };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createSiteConfOpLockPath() {
        String[] lockPath = { ScmLockPathDefine.SITE_CONF_OP_MUTEX };
        return new ScmLockPath(lockPath);
    }
    
    public static ScmLockPath createNodeConfOpLockPath() {
        String[] lockPath = { ScmLockPathDefine.NODE_CONF_OP_MUTEX };
        return new ScmLockPath(lockPath);
    }
    
    public static ScmLockPath createGlobalMetadataLockPath(String wsName) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.GLOBAL_METADATA_MUTEX };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createGlobalConfigPropLockPath() {
        String[] lockPath = { ScmLockPathDefine.CLOBAL_CONF_PROPS_MUTEX };
        return new ScmLockPath(lockPath);
    }

}
