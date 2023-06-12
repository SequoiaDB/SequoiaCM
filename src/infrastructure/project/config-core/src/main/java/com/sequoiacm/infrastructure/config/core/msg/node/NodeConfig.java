package com.sequoiacm.infrastructure.config.core.msg.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.NODE)
public class NodeConfig implements Config {
    @JsonProperty(FieldName.FIELD_CLCONTENTSERVER_ID)
    private int id;

    @JsonProperty(FieldName.FIELD_CLCONTENTSERVER_NAME)
    private String name;

    @JsonProperty(FieldName.FIELD_CLCONTENTSERVER_TYPE)
    private int type;

    @JsonProperty(FieldName.FIELD_CLCONTENTSERVER_SITEID)
    private int siteId;

    @JsonProperty(FieldName.FIELD_CLCONTENTSERVER_HOST_NAME)
    private String hostName;

    @JsonProperty(FieldName.FIELD_CLCONTENTSERVER_PORT)
    private int port;

    public NodeConfig() {
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getSiteId() {
        return siteId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getBusinessName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeConfig that = (NodeConfig) o;
        return id == that.id && type == that.type && siteId == that.siteId && port == that.port
                && Objects.equals(name, that.name) && Objects.equals(hostName, that.hostName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, siteId, hostName, port);
    }
}
