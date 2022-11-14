package com.sequoiacm.clean;

import java.util.concurrent.ThreadPoolExecutor;

public class ScmShutDownHook {
    
    private volatile boolean isShutdown = false;
    
    public ScmShutDownHook(final ThreadPoolExecutor threadPool) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                isShutdown = true;
                while (!threadPool.isTerminated()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }
    
    public boolean isShutdown() {
        return isShutdown;
    }
}
