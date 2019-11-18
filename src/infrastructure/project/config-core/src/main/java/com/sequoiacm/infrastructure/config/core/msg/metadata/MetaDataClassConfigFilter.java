package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class MetaDataClassConfigFilter {
    private String wsName;
    private String id;

    public MetaDataClassConfigFilter(String wsName, String id) {
        this.id = id;
        this.wsName = wsName;
    }

    public MetaDataClassConfigFilter(BSONObject obj) {
        wsName = BsonUtils.getStringChecked(obj, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        id = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_CLASS_ID);
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BSONObject toBSONObject() {
        BasicBSONObject classFilterObj = new BasicBSONObject(ScmRestArgDefine.META_DATA_CLASS_ID,
                id);
        classFilterObj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        return classFilterObj;
    }

}
