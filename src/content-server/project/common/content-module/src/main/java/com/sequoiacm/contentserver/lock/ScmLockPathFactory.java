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

    public static ScmLockPath createQuotaUsedLockPath(String type, String name) {
        String[] lockPath = { ScmLockPathDefine.QUOTA_USED, type, name };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createTagLibLockPath(String tagLib) {
        String[] lockPath = { ScmLockPathDefine.TAG_LIBS, tagLib,
                ScmLockPathDefine.TAG_LIB_GLOBAL_LOCK };
        return new ScmLockPath(lockPath);
    }

    public static ScmLockPath createWorkspaceTagRetrievalLock(String ws) {
        String[] lockPath = { ScmLockPathDefine.WORKSPACES, ws,
                ScmLockPathDefine.WORKSPACE_TAG_RETRIEVAL_LOCK };
        return new ScmLockPath(lockPath);
    }
}
