package com.sequoiacm.test.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShutdownHookMgr {

    private static volatile ShutdownHookMgr INSTANCE = null;
    private List<ShutdownHook> hookList = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean isTaskRunning;

    public static ShutdownHookMgr getInstance() {
        if (INSTANCE == null) {
            synchronized (ShutdownHookMgr.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ShutdownHookMgr();
                }
            }
        }

        return INSTANCE;
    }

    private ShutdownHookMgr() {
        // 增加一个关闭的钩子，在 JVM 关闭前执行此任务
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isTaskRunning = true;
            for (ShutdownHook shutdownHook : getHookListInOrder()) {
                shutdownHook.onShutdown();
            }
        }));
    }

    public boolean isShutDown() {
        return isTaskRunning;
    }

    public void addHook(ShutdownHook shutdownHook) {
        hookList.add(shutdownHook);
    }

    private List<ShutdownHook> getHookListInOrder() {
        // descending sort
        hookList.sort(Comparator.comparing(ShutdownHook::getPriority).reversed());
        return hookList;
    }

}
