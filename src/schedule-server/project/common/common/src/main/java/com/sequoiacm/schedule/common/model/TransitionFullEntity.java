package com.sequoiacm.schedule.common.model;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.types.BasicBSONList;
import org.springframework.beans.BeanUtils;

public class TransitionFullEntity extends TransitionUserEntity{
    private String id;

    private BasicBSONList workspaces;

    public BasicBSONList getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(BasicBSONList workspaces) {
        this.workspaces = workspaces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TransitionFullEntity() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME).append(":").append(getName())
                .append(",").append(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW).append(":")
                .append(getFlow()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT).append(":")
                .append(getExtraContent()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER).append(":")
                .append(getMatcher()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS).append(":")
                .append(getTransitionTriggers()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS).append(":")
                .append(getCleanTriggers()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_WORKSPACES).append(":")
                .append(getWorkspaces());
        return sb.toString();
    }

    @Override
    public TransitionFullEntity clone() {
        return new TransitionFullEntity(this);
    }

    private TransitionFullEntity(TransitionFullEntity entity) {
        this.id = entity.id;
        this.flow = new ScmFlow(entity.getFlow().getSource(), entity.getFlow().getDest());
        this.matcher = BsonUtils.deepCopyRecordBSON(entity.matcher);
        this.workspaces = BsonUtils.deepCopyBasicBSONList(entity.workspaces);
        this.transitionTriggers = new ScmTransitionTriggers(
                BsonUtils.deepCopyRecordBSON(entity.transitionTriggers.toBSONObj()));
        if (null != entity.cleanTriggers) {
            this.cleanTriggers = new ScmCleanTriggers(
                    BsonUtils.deepCopyRecordBSON(entity.cleanTriggers.toBSONObj()));
        }
        this.name = entity.name;

        this.extraContent = new ScmExtraContent(entity.getExtraContent().getDataCheckLevel(),
                entity.getExtraContent().getScope(), entity.getExtraContent().isRecycleSpace(),
                entity.getExtraContent().isQuickStart());
    }
}
