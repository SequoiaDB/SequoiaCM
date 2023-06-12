package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

@BusinessType(ScmBusinessTypeDefine.QUOTA)
public class QuotaNotifyOption implements NotifyOption {
    @JsonProperty(FieldName.Quota.TYPE)
    private String type;

    @JsonProperty(FieldName.Quota.NAME)
    private String name;

    @JsonProperty(ScmRestArgDefine.QUOTA_CONF_VERSION)
    private Integer version;

    public QuotaNotifyOption(String type, String name, Integer version) {
        this.type = type;
        this.name = name;
        this.version = version;
    }

    public QuotaNotifyOption(String businessName, Integer version) {
        this.type = QuotaConfig.getTypeFromBusinessName(businessName);
        this.name = QuotaConfig.getNameFromBusinessName(businessName);
        this.version = version;
    }

    public QuotaNotifyOption() {
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String getBusinessName() {
        return QuotaConfig.toBusinessName(type, name);
    }

    @Override
    public Version getBusinessVersion() {
        return new Version(ScmBusinessTypeDefine.QUOTA, getBusinessName(), version);
    }


    @Override
    public String toString() {
        return "QuotaNotifyOption{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", version=" + version + '}';
    }
}
