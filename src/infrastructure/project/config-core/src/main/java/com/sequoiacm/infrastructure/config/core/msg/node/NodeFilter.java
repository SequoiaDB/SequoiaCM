package com.sequoiacm.infrastructure.config.core.msg.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.NODE)
public class NodeFilter implements ConfigFilter {

    @JsonProperty(ScmRestArgDefine.NODE_CONF_NODEHOSTNAME)
    private String hostName;

    @JsonIgnore
    private String hostIp;

    @JsonProperty(ScmRestArgDefine.NODE_CONF_NODEPORT)
    private Integer port;

    public NodeFilter(String host, Integer port) {
        this.port = port;
        try {
            InetAddress addr = InetAddress.getByName(host);
            hostName = addr.getHostName();
            hostIp = addr.getHostAddress();
            // host is ip and ip/hostname is not exist
            if (hostName.equals(hostIp)) {
                throw new IllegalArgumentException(
                        "the host ip does not exist in hosts file for the config service, please check hosts file: configServer="
                                + InetAddress.getLocalHost().toString() + ", hostIp=" + host);
            }
        }
        catch (UnknownHostException e) {
            throw new IllegalArgumentException("failed to check node host: " + host, e);
        }

    }

    public NodeFilter() {
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public BSONObject asSdbCondition() {
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

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeFilter that = (NodeFilter) o;
        return Objects.equals(hostName, that.hostName) && Objects.equals(hostIp, that.hostIp) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName, hostIp, port);
    }
}
