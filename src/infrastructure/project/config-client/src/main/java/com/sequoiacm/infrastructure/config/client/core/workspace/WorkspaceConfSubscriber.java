package com.sequoiacm.infrastructure.config.client.core.workspace;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkspaceConfSubscriber implements ScmConfSubscriber {
    @Value("${spring.application.name}")
    private String myServiceName;
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceConfSubscriber.class);

    @Autowired(required = false)
    private BucketConfSubscriber bucketConfSubscriber;

    private ScmConfClient confClient;

    private Map<String, WorkspaceConfig> wsCache = new ConcurrentHashMap<>();
    private long heartbeatInterval;
    private DefaultVersionFilter versionFilter;

    @PostConstruct
    private void postConstruct() throws ScmConfigException {
        if (bucketConfSubscriber == null) {
            logger.info(
                    "bucket conf subscriber not found, workspace delete event will not clear bucket cache");
        }
        confClient.subscribeWithAsyncRetry(this);
    }

    @Autowired
    public WorkspaceConfSubscriber(WorkspaceConfSubscriberConfig config, ScmConfClient confClient)
            throws ScmConfigException {
        this.confClient = confClient;
        this.heartbeatInterval = config.getHeartbeatInterval();
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.WORKSPACE);
    }

    public WorkspaceConfig getWorkspace(String ws) throws ScmConfigException {
        WorkspaceConfig config = wsCache.get(ws);
        if (config != null) {
            return config;
        }

        return refreshWorkspaceCache(ws);
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.WORKSPACE;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive notification:" + notification);
        WorkspaceNotifyOption option = (WorkspaceNotifyOption) notification;
        String wsName = option.getWorkspaceName();
        if (notification.getEventType() == EventType.DELTE) {
            wsCache.remove(wsName);
            if (bucketConfSubscriber != null) {
                bucketConfSubscriber.invalidateBucketCacheByWs(wsName);
            }
            return;
        }
        refreshWorkspaceCache(wsName);
    }

    private WorkspaceConfig refreshWorkspaceCache(String wsName) throws ScmConfigException {
        WorkspaceConfig wsConfig = (WorkspaceConfig) confClient
                .getOneConf(ScmConfigNameDefine.WORKSPACE, new WorkspaceFilter(wsName));
        if (wsConfig != null) {
            wsCache.put(wsName, wsConfig);
        }
        else {
            wsCache.remove(wsName);
        }
        return wsConfig;
    }

    @Override
    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    @Override
    public long getHeartbeatIterval() {
        return this.heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        return new WorkspaceNotifyOption(version.getBussinessName(), version.getVersion(),
                eventType);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

}
