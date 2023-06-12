package com.sequoiacm.infrastructure.config.core.msg.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;

import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.NODE)
public class NodeNotifyOption implements NotifyOption {

    @JsonProperty(ScmRestArgDefine.NODE_CONF_NODENAME)
    private String nodeName;

    @JsonProperty(ScmRestArgDefine.NODE_CONF_NODEVERSION)
    private Integer version;

    public NodeNotifyOption(String nodeName, Integer version) {
        this.nodeName = nodeName;
        this.version = version;
    }

    public NodeNotifyOption() {
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getNodeName() {
        return nodeName;
    }

    @Override
    public Version getBusinessVersion() {
        return new Version(ScmBusinessTypeDefine.NODE, nodeName, version);
    }


    @Override
    public String toString() {
        return "nodeName=" + nodeName + ",version=" + version;
    }


    @Override
    public String getBusinessName() {
        return nodeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeNotifyOption that = (NodeNotifyOption) o;
        return Objects.equals(nodeName, that.nodeName) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeName, version);
    }
}
