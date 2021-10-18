package com.sequoiacm.test.common;

public abstract class ShutdownHook {

    private int priority;

    protected ShutdownHook(int priority) {
        this.priority = priority;
    }

    public abstract void onShutdown();

    public int getPriority() {
        return priority;
    }
}
