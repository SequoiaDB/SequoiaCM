package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;

public class OmStageTagBasic {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    public OmStageTagBasic(ScmLifeCycleStageTag lifeCycleStageTag) {
        this.name = lifeCycleStageTag.getName();
        this.description = lifeCycleStageTag.getDesc();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
