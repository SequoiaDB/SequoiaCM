package com.sequoiacm.config.server.module;

import java.util.List;
import java.util.Map;

import com.sequoiacm.config.server.common.ScmTargetType;

public class ScmConfPropsParam {
    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_TARGET_TYPE)
    private ScmTargetType targetType;

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_TARGETS)
    private List<String> targets;

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_PROPERTIES)
    private Map<String, String> updateProperties;

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_ACCEPT_UNKNOWN_PROPS)
    private boolean isAcceptUnrecognizedProp;

    private List<String> deleteProperties;

    public Map<String, String> getUpdateProperties() {
        return updateProperties;
    }

    public void setUpdateProperties(Map<String, String> updateProperties) {
        this.updateProperties = updateProperties;
    }

    public List<String> getDeleteProperties() {
        return deleteProperties;
    }

    public void setDeleteProperties(List<String> deleteProperties) {
        this.deleteProperties = deleteProperties;
    }

    public boolean isAcceptUnrecognizedProp() {
        return isAcceptUnrecognizedProp;
    }

    public void setAcceptUnrecognizedProp(boolean isAcceptUnrecognizedProp) {
        this.isAcceptUnrecognizedProp = isAcceptUnrecognizedProp;
    }

    public ScmTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ScmTargetType targetType) {
        this.targetType = targetType;
    }

    public void setTargetType(String targetType) {
        ScmTargetType t = ScmTargetType.valueOf(targetType);
        if (t == null) {
            throw new IllegalArgumentException("unknown target type:" + targetType);
        }
        this.targetType = t;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> tartgets) {
        this.targets = tartgets;
    }

}
