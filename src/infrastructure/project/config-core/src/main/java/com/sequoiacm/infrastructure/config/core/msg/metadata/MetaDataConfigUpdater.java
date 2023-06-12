package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.META_DATA)
public class MetaDataConfigUpdater implements ConfigUpdater {

    @JsonProperty(ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS)
    private MetaDataClassConfigUpdator classUpdator;

    @JsonProperty(ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE)
    private MetaDataAttributeConfigUpdator attributeUpdator;

    public MetaDataConfigUpdater(BSONObject configUpdator) {
        BSONObject obj = BsonUtils.getBSON(configUpdator,
                ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS);
        if (obj != null) {
            classUpdator = new MetaDataClassConfigUpdator(obj);
        }
        obj = BsonUtils.getBSON(configUpdator, ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE);
        if (obj != null) {
            attributeUpdator = new MetaDataAttributeConfigUpdator(obj);
        }
    }

    public MetaDataConfigUpdater() {
    }

    public MetaDataConfigUpdater(MetaDataAttributeConfigUpdator attributeUpdator) {
        this.attributeUpdator = attributeUpdator;
    }

    public MetaDataConfigUpdater(MetaDataClassConfigUpdator classUpdator) {
        this.classUpdator = classUpdator;
    }

    public MetaDataClassConfigUpdator getClassUpdator() {
        return classUpdator;
    }

    public MetaDataAttributeConfigUpdator getAttributeUpdator() {
        return attributeUpdator;
    }

    public void setClassUpdator(MetaDataClassConfigUpdator classUpdator) {
        this.classUpdator = classUpdator;
    }

    public void setAttributeUpdator(MetaDataAttributeConfigUpdator attributeUpdator) {
        this.attributeUpdator = attributeUpdator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaDataConfigUpdater that = (MetaDataConfigUpdater) o;
        return Objects.equals(classUpdator, that.classUpdator) && Objects.equals(attributeUpdator, that.attributeUpdator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classUpdator, attributeUpdator);
    }
}
