package com.sequoiacm.contentserver.bizconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataNotifyOption;

public class MetaDataConfSubscriber implements ScmConfSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MetaDataConfSubscriber.class);
    private long heartbeatInterval;
    private String myServiceName;
    private DefaultVersionFilter versionFilter;

    public MetaDataConfSubscriber(String myServiceName, long hearbeatInterval) {
        this.myServiceName = myServiceName;
        this.heartbeatInterval = hearbeatInterval;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.META_DATA);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.META_DATA;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive notification:" + notification);
        MetaDataNotifyOption metadataNotify = (MetaDataNotifyOption) notification;
        if (metadataNotify.getEventType() == EventType.DELTE) {
            MetaDataManager.getInstence().removeMetaDataByWsName(metadataNotify.getWsName());
        }
        else {
            MetaDataManager.getInstence().reloadMetaDataByWsName(metadataNotify.getWsName());
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
        return new MetaDataNotifyOption(version.getBussinessName(), eventType,
                version.getVersion());
    }

}
