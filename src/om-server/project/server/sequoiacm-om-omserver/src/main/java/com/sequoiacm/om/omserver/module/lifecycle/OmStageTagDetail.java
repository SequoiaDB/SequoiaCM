package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;

public class OmStageTagDetail extends OmStageTagBasic {

    @JsonProperty("bindingSite")
    private String site;

    public OmStageTagDetail(ScmLifeCycleStageTag lifeCycleStageTag, String site) {
        super(lifeCycleStageTag);
        this.site = site;
    }

    public void setSite(String site) {
        this.site = site;
    }
}
