package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;

import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.META_DATA)
public class MetaDataNotifyOption implements NotifyOption {

    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    private String wsName;

    @JsonProperty(ScmRestArgDefine.META_DATA_VERSION)
    private Integer version;

    public MetaDataNotifyOption(String wsName, Integer version) {
        this.wsName = wsName;
        this.version = version;
    }

    public MetaDataNotifyOption() {
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    @Override
    public Version getBusinessVersion() {
        return new Version(ScmBusinessTypeDefine.META_DATA, wsName, version);
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "MetaDataNotifyOption [wsName=" + wsName + ", version=" + version
                + "]";
    }

    @Override
    public String getBusinessName() {
        return wsName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetaDataNotifyOption that = (MetaDataNotifyOption) o;
        return Objects.equals(wsName, that.wsName) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, version);
    }
}
