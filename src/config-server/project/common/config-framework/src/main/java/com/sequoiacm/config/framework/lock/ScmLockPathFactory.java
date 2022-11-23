package com.sequoiacm.config.framework.lock;

public class ScmLockPathFactory {
    // 锁定全部 workspace 创建操作
    public static ScmLockPath createWorkspaceConfOpLockPath() {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES_CONF_OP_MUTEX };
        return new ScmLockPath(lockPath);
    }

    // 锁定单个 workspace
    public static ScmLockPath createWorkspaceConfOpLockPath(String wsName) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACE_CONF_OP_MUTEX , wsName};
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createWorkspaceExtraCsLockPath() {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES_CREATE_EXTRA_CS_MUTEX };
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
