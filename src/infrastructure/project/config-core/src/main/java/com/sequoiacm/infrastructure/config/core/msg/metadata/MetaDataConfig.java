package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class MetaDataConfig implements Config {

    private MetaDataClassConfig classConfig;
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
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (classConfig != null) {
            obj.put(ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS, classConfig.toBSONObject());
        }

        if (attributeConfig != null) {
            obj.put(ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE, attributeConfig.toBSONObject());
        }
        return obj;
    }

}
