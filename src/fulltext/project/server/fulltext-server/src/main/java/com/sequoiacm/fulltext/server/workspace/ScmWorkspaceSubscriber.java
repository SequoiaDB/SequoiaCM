package com.sequoiacm.fulltext.server.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;

public class ScmWorkspaceSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceSubscriber.class);
    private ScmWorkspaceMgr wsMgr;
    private long hearbeatInterval;

    public ScmWorkspaceSubscriber(ScmWorkspaceMgr wsMgr, long hearbeatInterval) {
        this.wsMgr = wsMgr;
        this.hearbeatInterval = hearbeatInterval;
    }

    @Override
    public String myServiceName() {
        return "fulltext-server";
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.WORKSPACE;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive workspace notification:{}", notification);
        WorkspaceNotifyOption wsNotify = (WorkspaceNotifyOption) notification;
        if (wsNotify.getEventType() == EventType.DELTE) {
            wsMgr.removeWs(wsNotify.getWorkspaceName());
            return;
        }

        if (wsNotify.getEventType() == EventType.CREATE) {
            wsMgr.addWs(wsNotify.getWorkspaceName());
            return;
        }

        if (wsNotify.getEventType() == EventType.UPDATE) {
            wsMgr.updateWs(wsNotify.getWorkspaceName());
            return;
        }

        throw new FullTextException(ScmError.INVALID_ARGUMENT,
                "unknown event type:" + notification);
    }

    @Override
    public VersionFilter getVersionFilter() {
        return new DefaultVersionFilter(ScmConfigNameDefine.WORKSPACE);
    }

    @Override
    public long getHeartbeatIterval() {
        return hearbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        DefaultVersion defaultVersion = (DefaultVersion) version;
        return new WorkspaceNotifyOption(defaultVersion.getBussinessName(),
                defaultVersion.getVersion(), eventType);
    }
}
