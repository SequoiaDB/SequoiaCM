package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class MetaDataNotifyCallback implements NotifyCallback {

    private static final Logger logger = LoggerFactory.getLogger(MetaDataNotifyCallback.class);


    @Override
    public void processNotify(EventType type, String businessName, NotifyOption notification)
            throws Exception {
        if (type == EventType.DELTE) {
            MetaDataManager.getInstence().removeMetaDataByWsName(businessName);
        }
        else {
            MetaDataManager.getInstence().reloadMetaDataByWsName(businessName);
        }
    }
}
