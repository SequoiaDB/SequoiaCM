package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.META_DATA)
public class MetaDataConfigFilter implements ConfigFilter {
    @JsonProperty(ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE)
    private MetaDataAttributeConfigFilter attributeFilter;

    @JsonProperty(ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS)
    private MetaDataClassConfigFilter classFilter;

    public MetaDataConfigFilter(BSONObject configFilter) {
        BSONObject obj = BsonUtils.getBSON(configFilter,
                ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS);
        if (obj != null) {
            classFilter = new MetaDataClassConfigFilter(obj);
        }
        obj = BsonUtils.getBSON(configFilter, ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE);
        if (obj != null) {
            attributeFilter = new MetaDataAttributeConfigFilter(obj);
        }
    }

    public MetaDataConfigFilter() {
    }

    public MetaDataConfigFilter(MetaDataAttributeConfigFilter attributeFilter) {
        this.attributeFilter = attributeFilter;
    }

    public MetaDataConfigFilter(MetaDataClassConfigFilter classFilter) {
        this.classFilter = classFilter;
    }

    public MetaDataAttributeConfigFilter getAttributeFilter() {
        return attributeFilter;
    }

    public MetaDataClassConfigFilter getClassFilter() {
        return classFilter;
    }

    public void setAttributeFilter(MetaDataAttributeConfigFilter attributeFilter) {
        this.attributeFilter = attributeFilter;
    }

    public void setClassFilter(MetaDataClassConfigFilter classFilter) {
        this.classFilter = classFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaDataConfigFilter that = (MetaDataConfigFilter) o;
        return Objects.equals(attributeFilter, that.attributeFilter) && Objects.equals(classFilter, that.classFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeFilter, classFilter);
    }
}
