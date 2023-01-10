package com.sequoiacm.om.omserver.module.lifecycle;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import org.bson.BSONObject;

public class OmTransitionFilter {

    private String nameMatcher;

    private String stageTag;

    public OmTransitionFilter(BSONObject transitionFilter) {
        this.nameMatcher = BsonUtils.getString(transitionFilter, "name_matcher");
        this.stageTag = BsonUtils.getString(transitionFilter, RestParamDefine.STAGE_TAG);
    }

    public String getNameMatcher() {
        return nameMatcher;
    }

    public String getStageTag() {
        return stageTag;
    }
}
