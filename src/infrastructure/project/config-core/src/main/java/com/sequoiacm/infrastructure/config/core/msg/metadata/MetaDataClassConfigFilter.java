package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

import java.util.Objects;

public class MetaDataClassConfigFilter {
    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    private String wsName;

    @JsonProperty(ScmRestArgDefine.META_DATA_CLASS_ID)
    private String id;

    @JsonProperty(ScmRestArgDefine.META_DATA_CLASS_NAME)
    private String name;

    public MetaDataClassConfigFilter(String wsName) {
        this.wsName = wsName;
    }

    public MetaDataClassConfigFilter() {
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

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetaDataClassConfigFilter that = (MetaDataClassConfigFilter) o;
        return Objects.equals(wsName, that.wsName) && Objects.equals(id, that.id)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, id, name);
    }
}
