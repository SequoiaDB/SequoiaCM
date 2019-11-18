package com.sequoiacm.infrastructure.common.timer;

public class ScmTimerFactory {
    public static ScmTimer createScmTimer() {
        return new ScmTimerThreadPoolImpl();
    }
}
