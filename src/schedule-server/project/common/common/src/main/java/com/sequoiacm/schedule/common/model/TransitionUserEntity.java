package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.BSONObject;

public class TransitionUserEntity {

    protected String name;
    protected ScmFlow flow;
    @JsonProperty("extra_content")
    protected ScmExtraContent extraContent;
    protected BSONObject matcher;
    @JsonProperty("transition_triggers")
    protected ScmTransitionTriggers transitionTriggers;
    @JsonProperty("clean_triggers")
    protected ScmCleanTriggers cleanTriggers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScmFlow getFlow() {
        return flow;
    }

    public void setFlow(ScmFlow flow) {
        this.flow = flow;
    }

    public ScmExtraContent getExtraContent() {
        return extraContent;
    }

    public void setExtraContent(ScmExtraContent extraContent) {
        this.extraContent = extraContent;
    }

    public BSONObject getMatcher() {
        return matcher;
    }

    public void setMatcher(BSONObject matcher) {
        this.matcher = matcher;
    }

    public ScmTransitionTriggers getTransitionTriggers() {
        return transitionTriggers;
    }

    public void setTransitionTriggers(ScmTransitionTriggers transitionTriggers) {
        this.transitionTriggers = transitionTriggers;
    }

    public ScmCleanTriggers getCleanTriggers() {
        return cleanTriggers;
    }

    public void setCleanTriggers(ScmCleanTriggers cleanTriggers) {
        this.cleanTriggers = cleanTriggers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME).append(":").append(getName())
                .append(",").append(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW).append(":")
                .append(getFlow().toBSONObj()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT).append(":")
                .append(getExtraContent().toBSONObj()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER).append(":")
                .append(getMatcher()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS).append(":")
                .append(getTransitionTriggers().toBSONObj());
        if (getCleanTriggers() != null) {
            sb.append(",").append(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS)
                    .append(":").append(getCleanTriggers().toBSONObj());
        }
        return sb.toString();
    }
}
