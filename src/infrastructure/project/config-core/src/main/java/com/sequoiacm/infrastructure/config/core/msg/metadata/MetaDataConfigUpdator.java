package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;

public class MetaDataConfigUpdator implements ConfigUpdator {
    private MetaDataClassConfigUpdator classUpdator;
    private MetaDataAttributeConfigUpdator attributeUpdator;

    public MetaDataConfigUpdator(BSONObject configUpdator) {
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

    public MetaDataConfigUpdator(MetaDataAttributeConfigUpdator attributeUpdator) {
        this.attributeUpdator = attributeUpdator;
    }

    public MetaDataConfigUpdator(MetaDataClassConfigUpdator classUpdator) {
        this.classUpdator = classUpdator;
    }

    public MetaDataClassConfigUpdator getClassUpdator() {
        return classUpdator;
    }

    public MetaDataAttributeConfigUpdator getAttributeUpdator() {
        return attributeUpdator;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (attributeUpdator != null) {
            obj.put(ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE,
                    attributeUpdator.toBSONObject());
        }

        if (classUpdator != null) {
            obj.put(ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS, classUpdator.toBSONObject());
        }
        return obj;
    }

}
