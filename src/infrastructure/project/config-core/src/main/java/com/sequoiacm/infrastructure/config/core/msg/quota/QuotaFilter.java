package com.sequoiacm.infrastructure.config.core.msg.quota;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

@BusinessType(ScmBusinessTypeDefine.QUOTA)
public class QuotaFilter implements ConfigFilter {

    @JsonProperty(FieldName.Quota.TYPE)
    private String type;

    @JsonProperty(FieldName.Quota.NAME)
    private String name;

    public QuotaFilter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public QuotaFilter() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "QuotaFilter{" + "type='" + type + '\'' + ", name='" + name + '\'' + '}';
    }
}
