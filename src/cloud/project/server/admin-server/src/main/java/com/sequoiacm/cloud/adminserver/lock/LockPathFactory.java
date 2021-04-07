package com.sequoiacm.cloud.adminserver.lock;

import com.sequoiacm.infrastructure.lock.ScmLockPath;
import org.springframework.stereotype.Component;

@Component
public class LockPathFactory {
    private static final String FILE_STATISTICS_DATA = "file_statistics_data";

    public ScmLockPath fileStatisticsLock(String type) {
        String[] lockPath = { FILE_STATISTICS_DATA, type };
        return new ScmLockPath(lockPath);
    }
}
