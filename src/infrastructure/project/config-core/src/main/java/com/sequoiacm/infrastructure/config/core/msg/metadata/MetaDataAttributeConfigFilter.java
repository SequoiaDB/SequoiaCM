package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;


import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

import java.util.Objects;

public class MetaDataAttributeConfigFilter {

    @JsonProperty(ScmRestArgDefine.META_DATA_ATTRIBUTE_ID)
    private String id;

    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    private String wsName;

    public MetaDataAttributeConfigFilter(String wsName, String id) {
        this.id = id;
        this.wsName = wsName;
    }

    public MetaDataAttributeConfigFilter() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaDataAttributeConfigFilter that = (MetaDataAttributeConfigFilter) o;
        return Objects.equals(id, that.id) && Objects.equals(wsName, that.wsName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, wsName);
    }
}
