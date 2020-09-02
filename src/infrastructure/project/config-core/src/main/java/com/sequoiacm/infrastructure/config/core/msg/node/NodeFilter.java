package com.sequoiacm.infrastructure.config.core.msg.node;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class NodeFilter implements ConfigFilter {

    private String hostName;
    private String hostIp;
    private Integer port;

    public NodeFilter(String hostName, String hostIp, Integer port) {
        this.hostName = hostName;
        this.hostIp = hostIp;
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONList hostList = new BasicBSONList();
        if (hostName != null) {
            hostList.add(hostName);
        }
        if (hostIp != null) {
            hostList.add(hostIp);

        }

        BasicBSONObject hostSet = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_IN,
                hostList);
        BasicBSONObject hostFilter = new BasicBSONObject(ScmRestArgDefine.NODE_CONF_NODEHOSTNAME,
                hostSet);
        if (port != null) {
            hostFilter.put(ScmRestArgDefine.NODE_CONF_NODEPORT, port);
        }
        return hostFilter;
    }
}
