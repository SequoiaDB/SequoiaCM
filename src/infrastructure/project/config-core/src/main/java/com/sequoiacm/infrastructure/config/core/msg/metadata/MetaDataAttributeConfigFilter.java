package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class MetaDataAttributeConfigFilter {

    private String id;
    private String wsName;

    public MetaDataAttributeConfigFilter(String wsName, String id) {
        this.id = id;
        this.wsName = wsName;
    }

    public MetaDataAttributeConfigFilter(BSONObject obj) {
        id = BsonUtils.getStringChecked(obj, ScmRestArgDefine.META_DATA_ATTRIBUTE_ID);
        wsName = BsonUtils.getStringChecked(obj, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public BSONObject toBSONObject() {
        BasicBSONObject attributeFilterObj = new BasicBSONObject(
                ScmRestArgDefine.META_DATA_ATTRIBUTE_ID, id);
        attributeFilterObj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        return attributeFilterObj;
    }

}
