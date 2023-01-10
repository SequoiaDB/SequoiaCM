package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class ScmExtraContent {
    @JsonProperty("data_check_level")
    private String dataCheckLevel;
    private String scope;
    @JsonProperty("recycle_space")
    private boolean recycleSpace;
    @JsonProperty("quick_start")
    private boolean quickStart;

    public ScmExtraContent(BSONObject obj) {
        this.quickStart = BsonUtils.getBoolean(obj,
                FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_QUICK_START);
        this.dataCheckLevel = BsonUtils.getString(obj,
                FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_DATA_CHECK_LEVEL);
        this.scope = BsonUtils.getString(obj, FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_SCOPE);
        this.recycleSpace = BsonUtils.getBoolean(obj,
                FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_RECYCLE_SPACE);
    }

    public ScmExtraContent(String dataCheckLevel, String scope, boolean recycleSpace,
            boolean quickStart) {
        this.dataCheckLevel = dataCheckLevel;
        this.scope = scope;
        this.recycleSpace = recycleSpace;
        this.quickStart = quickStart;
    }

    public String getDataCheckLevel() {
        return dataCheckLevel;
    }

    public void setDataCheckLevel(String dataCheckLevel) {
        this.dataCheckLevel = dataCheckLevel;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isRecycleSpace() {
        return recycleSpace;
    }

    public void setRecycleSpace(boolean recycleSpace) {
        this.recycleSpace = recycleSpace;
    }

    public boolean isQuickStart() {
        return quickStart;
    }

    public void setQuickStart(boolean quickStart) {
        this.quickStart = quickStart;
    }

    public BSONObject toBSONObj() {
        BSONObject obj = new BasicBSONObject();
        obj.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_DATA_CHECK_LEVEL, dataCheckLevel);
        obj.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_QUICK_START, quickStart);
        obj.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_SCOPE, scope);
        obj.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_RECYCLE_SPACE, recycleSpace);
        return obj;
    }
}
