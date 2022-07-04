package com.sequoiacm.infrastructure.common.timer;

public class ScmTimerFactory {
    public static ScmTimer createScmTimer() {
        return new ScmTimerThreadPoolImpl();
    }

    public static ScmTimer createScmTimer(String name) {
        return new ScmTimerThreadPoolImpl(name);
    }
}
