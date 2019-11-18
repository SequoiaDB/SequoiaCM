package com.sequoiacm.infrastructure.config.core.msg.node;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class NodeFilter implements ConfigFilter {

    private String hostName;
    private Integer port;

    public NodeFilter(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (hostName != null) {
            obj.put(ScmRestArgDefine.NODE_CONF_NODEHOSTNAME, hostName);
        }
        if (port != null) {
            obj.put(ScmRestArgDefine.NODE_CONF_NODEPORT, port);
        }
        return obj;
    }
}
