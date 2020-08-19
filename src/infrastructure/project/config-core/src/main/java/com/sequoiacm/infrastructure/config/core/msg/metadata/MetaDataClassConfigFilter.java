package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class MetaDataClassConfigFilter {
    private String wsName;
    private String id;
    private String name;

    public MetaDataClassConfigFilter(String wsName) {
        this.wsName = wsName;
    }

    public MetaDataClassConfigFilter appendId(String id) {
        this.id = id;
        return this;
    }

    public MetaDataClassConfigFilter appendName(String name) {
        this.name = name;
        return this;
    }

    public MetaDataClassConfigFilter(BSONObject obj) {
        wsName = BsonUtils.getStringChecked(obj, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        id = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_CLASS_ID);
        name = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_CLASS_NAME);
    }

    public String getWsName() {
        return wsName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BSONObject toBSONObject() {
        BasicBSONObject classFilterObj = new BasicBSONObject(ScmRestArgDefine.META_DATA_CLASS_ID,
                id);
        classFilterObj.put(ScmRestArgDefine.META_DATA_CLASS_NAME, name);
        classFilterObj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        return classFilterObj;
    }
}
