package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class SiteConfNotifyCallback implements NotifyCallback {
    private static final Logger logger = LoggerFactory.getLogger(SiteConfNotifyCallback.class);

    @Override
    public void processNotify(EventType type, String businessName, NotifyOption notification)
            throws Exception {
        if (type == EventType.DELTE) {
            ScmContentModule.getInstance().removeSite(businessName);
            return;
        }
        ScmContentModule.getInstance().reloadSite(businessName);
    }
}
