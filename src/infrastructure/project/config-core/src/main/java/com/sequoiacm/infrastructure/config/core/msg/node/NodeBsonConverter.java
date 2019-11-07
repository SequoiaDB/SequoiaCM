package com.sequoiacm.infrastructure.config.core.msg.node;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.Converter;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Converter
public class NodeBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        NodeConfig nodeConfig = new NodeConfig();
        Integer nodeId = BsonUtils.getInteger(config, FieldName.FIELD_CLCONTENT_SERVER_ID);
        if (nodeId != null) {
            nodeConfig.setId(nodeId);
        }

        String name = BsonUtils.getStringChecked(config, FieldName.FIELD_CLCONTENT_SERVER_NAME);
        nodeConfig.setName(name);

        Integer nodeType = BsonUtils.getIntegerChecked(config,
                FieldName.FIELD_CLCONTENT_SERVER_TYPE);
        nodeConfig.setType(nodeType);

        Integer siteId = BsonUtils.getIntegerChecked(config,
                FieldName.FIELD_CLCONTENT_SERVER_SITE_ID);
        nodeConfig.setSiteId(siteId);

        String hostName = BsonUtils.getStringChecked(config,
                FieldName.FIELD_CLCONTENT_SERVER_HOST_NAME);
        nodeConfig.setHostName(hostName);

        Integer port = BsonUtils.getIntegerChecked(config, FieldName.FIELD_CLCONTENT_SERVER_PORT);
        nodeConfig.setPort(port);

        return nodeConfig;
    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) throws ScmConfigException {
        String hostName = BsonUtils.getString(configFilter,
                ScmRestArgDefine.NODE_CONF_NODEHOSTNAME);
        int port = BsonUtils.getInteger(configFilter, ScmRestArgDefine.NODE_CONF_NODEPORT);
        String hostIp;
        try {
            InetAddress addr = InetAddress.getByName(hostName);
            hostIp = addr.getHostAddress();
        }
        catch (UnknownHostException e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "host name is not exist in hosts file, please check hosts file: hostName="
                            + hostName);
        }
        return new NodeFilter(hostName, hostIp, port);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject configOption) {
        Integer version = BsonUtils.getInteger(configOption,
                ScmRestArgDefine.NODE_CONF_NODEVERSION);
        String nodeName = BsonUtils.getStringChecked(configOption,
                ScmRestArgDefine.NODE_CONF_NODENAME);
        return new NodeNotifyOption(nodeName, version, type);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdatorObj) {
        throw new IllegalArgumentException("unsupport to update node info");
    }

    @Override
    public VersionFilter convertToVersionFilter(BSONObject versionFilter) {
        return new DefaultVersionFilter(versionFilter);
    }

    @Override
    public Version convertToVersion(BSONObject version) {
        return new DefaultVersion(version);
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.NODE;
    }

}
