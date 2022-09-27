package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class OmConfPropsParam {

    @JsonProperty("target_type")
    private String targetType;

    @JsonProperty("targets")
    private List<String> targets;

    @JsonProperty("update_properties")
    private Map<String, String> updateProperties;

    @JsonProperty("delete_properties")
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

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }
}