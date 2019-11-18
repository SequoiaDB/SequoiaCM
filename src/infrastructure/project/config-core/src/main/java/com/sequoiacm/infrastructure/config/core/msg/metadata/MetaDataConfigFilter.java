package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class MetaDataConfigFilter implements ConfigFilter {
    private MetaDataAttributeConfigFilter attributeFilter;
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

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (classFilter != null) {
            obj.put(ScmRestArgDefine.META_DATA_CONF_TYPE_CLASS, classFilter.toBSONObject());
        }

        if (attributeFilter != null) {
            obj.put(ScmRestArgDefine.META_DATA_CONF_TYPE_ATTRIBUTE, attributeFilter.toBSONObject());
        }
        return obj;
    }

}
