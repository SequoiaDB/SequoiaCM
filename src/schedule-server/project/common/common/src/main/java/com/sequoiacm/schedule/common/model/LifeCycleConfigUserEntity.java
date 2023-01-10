package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.types.BasicBSONList;

public class LifeCycleConfigUserEntity {
    @JsonProperty("stage_tag")
    protected BasicBSONList stageTagConfig;

    @JsonProperty("transition")
    protected BasicBSONList transitionConfig;

    public LifeCycleConfigUserEntity() {
    }

    public LifeCycleConfigUserEntity(BasicBSONList stageTagConfig, BasicBSONList transitionConfig) {
        this.stageTagConfig = stageTagConfig;
        this.transitionConfig = transitionConfig;
    }

    public BasicBSONList getStageTagConfig() {
        return stageTagConfig;
    }

    public void setStageTagConfig(BasicBSONList stageTagConfig) {
        this.stageTagConfig = stageTagConfig;
    }

    public BasicBSONList getTransitionConfig() {
        return transitionConfig;
    }

    public void setTransitionConfig(BasicBSONList transitionConfig) {
        this.transitionConfig = transitionConfig;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_CONFIG).append(":")
                .append(getStageTagConfig()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_CONFIG).append(":")
                .append(getTransitionConfig());
        return sb.toString();
    }
}
