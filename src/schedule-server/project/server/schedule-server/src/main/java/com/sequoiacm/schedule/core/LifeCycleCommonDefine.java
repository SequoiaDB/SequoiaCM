package com.sequoiacm.schedule.core;

import com.sequoiacm.infrastructure.lock.ScmLockPath;

public class LifeCycleCommonDefine {
    public static ScmLockPath GLOBAL_LIFE_CYCLE_LOCK_PATH = new ScmLockPath(
            new String[] { "global_life_cycle" });
}
