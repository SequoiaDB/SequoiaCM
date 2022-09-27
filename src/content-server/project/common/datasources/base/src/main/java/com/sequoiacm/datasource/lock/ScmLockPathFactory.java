package com.sequoiacm.datasource.lock;

import com.sequoiacm.infrastructure.lock.ScmLockPath;

public class ScmLockPathFactory {

    public static ScmLockPath createDataTableLockPath(String siteName, String tableName) {
        String[] lockPath = { ScmLockPathDefine.DATASOURCE, siteName, ScmLockPathDefine.DATA_TABLE,
                tableName };
        return new ScmLockPath(lockPath);
    }
}
