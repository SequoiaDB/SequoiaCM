package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class NodeConfNotifyCallback implements NotifyCallback {
    private static final Logger logger = LoggerFactory.getLogger(NodeConfNotifyCallback.class);


    @Override
    public void processNotify(EventType type, String businessName, NotifyOption notification)
            throws Exception {
        if (type == EventType.DELTE) {
            ScmContentModule.getInstance().removeNode(businessName);
            return;
        }
        ScmContentModule.getInstance().reloadNode(businessName);
    }
}
