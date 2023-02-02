package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;

import java.util.List;

public class OmStageTagDetail extends OmStageTagBasic {

    @JsonProperty("bindingSite")
    private List<String> site;

    public OmStageTagDetail(ScmLifeCycleStageTag lifeCycleStageTag, List<String> site) {
        super(lifeCycleStageTag);
        this.site = site;
    }

    public void setSite(List<String> site) {
        this.site = site;
    }
}
