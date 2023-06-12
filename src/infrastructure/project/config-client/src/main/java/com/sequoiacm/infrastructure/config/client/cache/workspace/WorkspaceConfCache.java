package com.sequoiacm.infrastructure.config.client.cache.workspace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;

public class WorkspaceConfCache {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceConfCache.class);

    @Autowired
    private ScmConfClient confClient;

    private final Map<String, WorkspaceConfig> wsCache = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() throws ScmConfigException {
        confClient.subscribe(ScmBusinessTypeDefine.WORKSPACE, new NotifyCallback() {
            @Override
            public void processNotify(EventType type, String businessName,
                    NotifyOption notification) {
                onNotify(type, businessName, notification);
            }

            @Override
            public int priority() {
                return NotifyCallback.HIGHEST_PRECEDENCE;
            }
        });
    }

    @Autowired
    public WorkspaceConfCache(ScmConfClient confClient) {
        this.confClient = confClient;
    }

    public WorkspaceConfig getWorkspace(String ws) throws ScmConfigException {
        WorkspaceConfig config = wsCache.get(ws);
        if (config != null) {
            return config;
        }

        return refreshWorkspaceCache(ws);
    }

    public void onNotify(EventType type, String businessName, NotifyOption notification) {
        if (type == EventType.DELTE) {
            wsCache.remove(businessName);
            return;
        }
        try {
            refreshWorkspaceCache(businessName);
        }
        catch (Exception e) {
            logger.warn(
                    "fail to refresh workspace cache: eventType={}, businessName={}, notification={}",
                    type, businessName, notification, e);
            wsCache.remove(businessName);
        }
    }

    private WorkspaceConfig refreshWorkspaceCache(String wsName) throws ScmConfigException {
        WorkspaceConfig wsConfig = (WorkspaceConfig) confClient
                .getOneConf(ScmBusinessTypeDefine.WORKSPACE, new WorkspaceFilter(wsName));
        if (wsConfig != null) {
            wsCache.put(wsName, wsConfig);
        }
        else {
            wsCache.remove(wsName);
        }
        return wsConfig;
    }
}
