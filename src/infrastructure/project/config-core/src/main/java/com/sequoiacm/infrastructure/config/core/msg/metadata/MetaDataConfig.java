package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Config;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.META_DATA)
public class MetaDataConfig implements Config {

    @JsonProperty(ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS)
    private MetaDataClassConfig classConfig;

    @JsonProperty(ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE)
    private MetaDataAttributeConfig attributeConfig;

    public MetaDataConfig(BSONObject config) {
        BSONObject obj = BsonUtils.getBSON(config, ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS);
        if (obj != null) {
            classConfig = new MetaDataClassConfig(obj);
        }
        obj = BsonUtils.getBSON(config, ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE);
        if (obj != null) {
            attributeConfig = new MetaDataAttributeConfig(obj);
        }
    }

    public MetaDataConfig() {
    }

    public MetaDataConfig(MetaDataClassConfig classConfig) {
        this.classConfig = classConfig;
    }

    public MetaDataConfig(MetaDataAttributeConfig attributeConfig) {
        this.attributeConfig = attributeConfig;
    }

    public MetaDataClassConfig getClassConfig() {
        return classConfig;
    }

    public void setClassConfig(MetaDataClassConfig classConfig) {
        this.classConfig = classConfig;
    }

    public MetaDataAttributeConfig getAttributeConfig() {
        return attributeConfig;
    }

    public void setAttributeConfig(MetaDataAttributeConfig attributeConfig) {
        this.attributeConfig = attributeConfig;
    }

    @Override
    public String getBusinessName() {
        if (classConfig != null) {
            return classConfig.getWsName();
        }

        if (attributeConfig != null) {
            return attributeConfig.getWsName();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaDataConfig that = (MetaDataConfig) o;
        return Objects.equals(classConfig, that.classConfig) && Objects.equals(attributeConfig, that.attributeConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classConfig, attributeConfig);
    }
}
