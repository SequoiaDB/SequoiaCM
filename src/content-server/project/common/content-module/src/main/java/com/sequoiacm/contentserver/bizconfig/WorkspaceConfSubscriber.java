package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.site.ScmContentModule;
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
    private final BucketInfoManager bucketInfoMgr;
    private long heartbeatInterval;
    private DefaultVersionFilter versionFilter;
    private String myServiceName;

    public WorkspaceConfSubscriber(BucketInfoManager bucketInfoManager, String myServiceName,
            long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        this.myServiceName = myServiceName;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.WORKSPACE);
        this.bucketInfoMgr = bucketInfoManager;
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
            ScmContentModule.getInstance().removeWorkspace(wsName);
            MetaDataManager.getInstence().removeMetaDataByWsName(wsName);
            bucketInfoMgr.invalidateBucketCacheByWs(wsName);
            return;
        }
        if (notification.getEventType() == EventType.CREATE) {
            MetaDataManager.getInstence().reloadMetaDataByWsName(wsName);
        }
        ScmContentModule.getInstance().reloadWorkspace(wsName);
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
    public long getInitStatusInterval() {
        return 2 * 1000;
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
