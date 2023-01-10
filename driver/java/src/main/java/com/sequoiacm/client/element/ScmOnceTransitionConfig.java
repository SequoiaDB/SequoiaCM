package com.sequoiacm.client.element;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

public class ScmOnceTransitionConfig extends ScmFileTaskConfigBase {

    private boolean isRecycleSpace;

    private String sourceStageTag;

    private String destStageTag;

    private String preferredRegion;

    private String preferredZone;

    private String type;

    public ScmOnceTransitionConfig(String type, ScmWorkspace workspace,
            long maxExecTime, BSONObject condition, String sourceStageTag, String destStageTag,
            String preferredRegion, String preferredZone) {
        this.isRecycleSpace = true;
        this.setQuickStart(false);
        this.setDataCheckLevel(ScmDataCheckLevel.STRICT);
        this.setScope(ScmType.ScopeType.SCOPE_ALL);
        this.setWorkspace(workspace);
        this.setMaxExecTime(maxExecTime);
        this.setCondition(condition);
        this.sourceStageTag = sourceStageTag;
        this.destStageTag = destStageTag;
        this.preferredRegion = preferredRegion;
        this.preferredZone = preferredZone;
        this.type = type;
    }

    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    public void setRecycleSpace(boolean recycleSpace) {
        isRecycleSpace = recycleSpace;
    }

    public String getSourceStageTag() {
        return sourceStageTag;
    }

    public void setSourceStageTag(String sourceStageTag) {
        this.sourceStageTag = sourceStageTag;
    }

    public String getDestStageTag() {
        return destStageTag;
    }

    public void setDestStageTag(String destStageTag) {
        this.destStageTag = destStageTag;
    }

    public String getPreferredRegion() {
        return preferredRegion;
    }

    public void setPreferredRegion(String preferredRegion) {
        this.preferredRegion = preferredRegion;
    }

    public String getPreferredZone() {
        return preferredZone;
    }

    public void setPreferredZone(String preferredZone) {
        this.preferredZone = preferredZone;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ScmOnceTransitionConfig{" + "isRecycleSpace=" + isRecycleSpace + "preferredRegion="
                + preferredRegion + "preferredZone=" + preferredZone + "TaskType=" + type
                + ", sourceStageTag='"
                + sourceStageTag + ", destStageTag='" + destStageTag + '\'' + "} "
                + super.toString();
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bsonObject = super.getBSONObject();
        bsonObject.put(FieldName.Task.FIELD_OPTION_IS_RECYCLE_SPACE, isRecycleSpace);
        bsonObject.put("source", sourceStageTag);
        bsonObject.put("dest", destStageTag);
        return bsonObject;
    }
}
