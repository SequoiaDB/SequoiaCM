package com.sequoiacm.cloud.adminserver.lock;

import com.sequoiacm.infrastructure.lock.ScmLockPath;
import org.springframework.stereotype.Component;

@Component
public class LockPathFactory {
    private static final String FILE_STATISTICS_DATA = "file_statistics_data";

    private static final String OBJECT_DELTA_STATISTICS = "object_delta_statistics";

    private static final String QUOTA_MANAGE = "quota_manager";
    private static final String QUOTA_SYNC = "quota_sync_task";
    private static final String QUOTA_USED = "quota_used";

    public ScmLockPath fileStatisticsLock(String type) {
        String[] lockPath = { FILE_STATISTICS_DATA, type };
        return new ScmLockPath(lockPath);
    }

    public ScmLockPath objectDeltaStatisticsLock(String bucketName) {
        String[] lockPath = { OBJECT_DELTA_STATISTICS, bucketName };
        return new ScmLockPath(lockPath);
    }

    public ScmLockPath quotaManageLockPath(String type, String name) {
        String[] lockPath = { QUOTA_MANAGE, type, name };
        return new ScmLockPath(lockPath);
    }

    public ScmLockPath quotaUsedLockPath(String type, String name) {
        String[] lockPath = { QUOTA_USED, type, name };
        return new ScmLockPath(lockPath);
    }

    public ScmLockPath quotaSyncPath(String type, String name) {
        String[] lockPath = { QUOTA_SYNC, type, name };
        return new ScmLockPath(lockPath);
    }
}
