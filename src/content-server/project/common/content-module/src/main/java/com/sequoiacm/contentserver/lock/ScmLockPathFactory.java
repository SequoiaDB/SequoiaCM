package com.sequoiacm.contentserver.lock;

public class ScmLockPathFactory {

    public static ScmLockPath createBPLockPath(String wsName, String breakpointFileName) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.BREAKPOINT_FILES, breakpointFileName };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createFileLockPath(String wsName, String fileId) {
        String[] lockPath = new String[] { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.FILES, fileId };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createFileContentLockPath(String wsName, String siteName,
            String dataId) {
        String[] lockPath = new String[] { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.SITES, siteName, ScmLockPathDefine.FILE_CONTENT, dataId };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createBatchLockPath(String wsName, String batchId) {
        String[] lockPath = new String[] { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.BATCHES, batchId };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createDirLockPath(String wsName, String dirId) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES, wsName, ScmLockPathDefine.DIRECTORIES,
                dirId };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createGlobalDirLockPath(String wsName) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.GLOBAL_DIRECTORIES_MUTEX };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createWorkspaceConfOpLockPath() {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES_CONF_OP_MUTEX};
        return new ScmLockPath(lockPath);
    }
    
    public static ScmLockPath createGlobalMetadataLockPath(String wsName) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES, wsName,
                ScmLockPathDefine.GLOBAL_METADATA_MUTEX };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createDataTableLockPath(String siteName, String tableName) {
        String[] lockPath = { ScmLockPathDefine.DATASOURCE, siteName, ScmLockPathDefine.DATA_TABLE,
                tableName };
        return new ScmLockPath(lockPath);
    }
}
