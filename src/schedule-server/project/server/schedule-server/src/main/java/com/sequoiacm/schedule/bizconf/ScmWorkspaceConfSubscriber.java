package com.sequoiacm.schedule.bizconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.ScheduleServer;

public class ScmWorkspaceConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceConfSubscriber.class);
    private DefaultVersionFilter versionFilter;
    private long heartbeatInterval;
    private String myServiceName;

    public ScmWorkspaceConfSubscriber(String myServiceName, long hearbeatInterval) {
        this.myServiceName = myServiceName;
        this.heartbeatInterval = hearbeatInterval;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.WORKSPACE);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.WORKSPACE;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive notification:" + notification);
        WorkspaceNotifyOption wsNotification = (WorkspaceNotifyOption) notification;
        if (notification.getEventType() == EventType.DELTE) {
            ScheduleServer.getInstance().removeWorkspace(wsNotification.getWorkspaceName());
            ScheduleMgrWrapper.getInstance()
                    .deleteScheduleByWorkspace(wsNotification.getWorkspaceName());
        }
        else {
            ScheduleServer.getInstance().reloadWorkspace(wsNotification.getWorkspaceName());
        }
    }

    @Override
    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    @Override
    public long getHeartbeatIterval() {
        return heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        return new WorkspaceNotifyOption(version.getBussinessName(), version.getVersion(),
                eventType);
    }

}
