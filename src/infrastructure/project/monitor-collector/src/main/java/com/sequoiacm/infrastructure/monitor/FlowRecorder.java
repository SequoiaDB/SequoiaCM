package com.sequoiacm.infrastructure.monitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

public class FlowRecorder {
    private static final Logger logger = LoggerFactory.getLogger(FlowRecorder.class);

    private static FlowRecorder recorder = new FlowRecorder();
    private Map<String, WorkspaceFlow> workspaceFlowMap = new HashMap<String, WorkspaceFlow>();

    private FlowRecorder() {
    }

    public static FlowRecorder getInstance() {
        return recorder;
    }

    private synchronized WorkspaceFlow getOrCreateWorkspaceFlow(String workspaceName) {
        WorkspaceFlow f = workspaceFlowMap.get(workspaceName);
        if (f == null) {
            f = new WorkspaceFlow(workspaceName);
            workspaceFlowMap.put(workspaceName, f);
        }

        return f;
    }

    public synchronized void removeWorkspaceFlow(String workspaceName) {
        workspaceFlowMap.remove(workspaceName);
    }

    public void addUploadSize(String workspaceName, long size) {
        try {
            WorkspaceFlow f = getOrCreateWorkspaceFlow(workspaceName);
            f.addUploadSize(size);
        }
        catch (Exception e) {
            logger.warn("add upload size failed:workspace={},size={}", workspaceName, size, e);
        }
    }

    public void addDownloadSize(String workspaceName, long size) {
        try {
            WorkspaceFlow f = getOrCreateWorkspaceFlow(workspaceName);
            f.addDownloadSize(size);
        }
        catch (Exception e) {
            logger.warn("add download size failed:workspace={},size={}", workspaceName, size, e);
        }
    }

    public Collection<WorkspaceFlow> getFlow() {
        return workspaceFlowMap.values();
    }
}
