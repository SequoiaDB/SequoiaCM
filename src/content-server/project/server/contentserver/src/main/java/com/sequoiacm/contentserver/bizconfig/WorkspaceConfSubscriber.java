package com.sequoiacm.contentserver.bizconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;

public class WorkspaceConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceConfSubscriber.class);
    private long heartbeatInterval;
    private DefaultVersionFilter versionFilter;
    private String myServiceName;

    public WorkspaceConfSubscriber(String myServiceName, long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        this.myServiceName = myServiceName;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.WORKSPACE);
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
            ScmContentServer.getInstance().removeWorkspace(wsName);
            MetaDataManager.getInstence().removeMetaDataByWsName(wsName);
            return;
        }
        if (notification.getEventType() == EventType.CREATE) {
            MetaDataManager.getInstence().reloadMetaDataByWsName(wsName);
        }
        ScmContentServer.getInstance().reloadWorkspace(wsName);
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
