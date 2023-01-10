package com.sequoiacm.schedule.core.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class WorkspaceMgr {
    private Map<String, WorkspaceInfo> workspaceMap;
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void init(Map<String, WorkspaceInfo> initWorkspaceInfoMap) {
        this.workspaceMap = initWorkspaceInfoMap;
    }

    public void reloadWorkspace(WorkspaceInfo wsInfo) {
        WriteLock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            workspaceMap.put(wsInfo.getName(), wsInfo);
        }
        finally {
            wLock.unlock();
        }
    }

    public void removeWorkspace(String wsName) {
        WriteLock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            workspaceMap.remove(wsName);
        }
        finally {
            wLock.unlock();
        }
    }

    public WorkspaceInfo getWsInfo(String name) {
        ReadLock rLock = rwLock.readLock();
        rLock.lock();
        try {
            return workspaceMap.get(name);
        }
        finally {
            rLock.unlock();
        }
    }

    public List<WorkspaceInfo> getAllWsInfo(){
        ReadLock rLock = rwLock.readLock();
        rLock.lock();
        try {
            return new ArrayList<>(workspaceMap.values());
        }
        finally {
            rLock.unlock();
        }
    }

}
