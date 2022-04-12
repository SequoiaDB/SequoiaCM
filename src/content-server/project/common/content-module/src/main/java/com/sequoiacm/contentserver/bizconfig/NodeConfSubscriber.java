package com.sequoiacm.contentserver.bizconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeNotifyOption;

public class NodeConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(NodeConfSubscriber.class);
    private long heartbeatInterval;
    private DefaultVersionFilter versionFilter;
    private String myServiceName;

    public NodeConfSubscriber(String myServiceName, long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        this.myServiceName = myServiceName;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.NODE);
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.NODE;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive notification:" + notification);
        NodeNotifyOption option = (NodeNotifyOption) notification;
        String nodeName = option.getNodeName();
        if (notification.getEventType() == EventType.DELTE) {
            ScmContentModule.getInstance().removeNode(nodeName);
            return;
        }
        ScmContentModule.getInstance().reloadNode(nodeName);
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
        return new NodeNotifyOption(version.getBussinessName(), version.getVersion(), eventType);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

}
