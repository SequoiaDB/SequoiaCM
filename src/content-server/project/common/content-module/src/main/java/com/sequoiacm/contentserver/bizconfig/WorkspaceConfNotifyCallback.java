package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.tag.TagLibMgr;
import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class WorkspaceConfNotifyCallback implements NotifyCallback {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceConfNotifyCallback.class);
    private final TagLibMgr tagLibMgr;

    public WorkspaceConfNotifyCallback(TagLibMgr tagLibMgr) {
        this.tagLibMgr = tagLibMgr;
    }

    @Override
    public void processNotify(EventType type, String wsName, NotifyOption notification)
            throws Exception {
        if (type == EventType.DELTE) {
            ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfo(wsName);
            ScmContentModule.getInstance().removeWorkspace(wsName);
            MetaDataManager.getInstence().removeMetaDataByWsName(wsName);
            FlowRecorder.getInstance().removeWorkspaceFlow(wsName);
            tagLibMgr.invalidateTagCacheByWs(ws);
            return;
        }
        if (type == EventType.CREATE) {
            MetaDataManager.getInstence().reloadMetaDataByWsName(wsName);
        }
        ScmContentModule.getInstance().reloadWorkspace(wsName);
    }

    @Override
    public int priority() {
        return NotifyCallback.HIGHEST_PRECEDENCE;
    }
}
