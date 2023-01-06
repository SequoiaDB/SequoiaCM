package com.sequoiacm.cephs3.lock;

import com.sequoiacm.infrastructure.lock.ScmLockPath;

/**
 * bucketName lock path
 */
public class BucketNameLockPathFactory {
    private static final String WORKSPACE = "workspace";
    private static final String SITE = "site";

    public static ScmLockPath createBucketNameLockPath(String workspaceName, int siteId) {
        String[] lockPath = { WORKSPACE, workspaceName, SITE, String.valueOf(siteId), "bucket" };
        return new ScmLockPath(lockPath);
    }

}
