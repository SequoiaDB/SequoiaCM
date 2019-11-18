package com.sequoiacm.config.server.module;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class ScmUpdateConfPropsResultSet {
    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_SET_SUCCESS)
    private List<ScmUpdateConfPropsResult> successes = new ArrayList<>();

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_SET_FAILES)
    private List<ScmUpdateConfPropsResult> failes = new ArrayList<>();

    public void addResult(ScmUpdateConfPropsResult result) {
        if (result.getErrorMessage() != null) {
            failes.add(result);
        }
        else {
            successes.add(result);
        }
    }

    public void addResults(List<ScmUpdateConfPropsResult> results) {
        for (ScmUpdateConfPropsResult result : results) {
            addResult(result);
        }
    }

    public List<ScmUpdateConfPropsResult> getSuccesses() {
        return successes;
    }

    public void setSuccesses(List<ScmUpdateConfPropsResult> successes) {
        this.successes = successes;
    }

    public List<ScmUpdateConfPropsResult> getFailes() {
        return failes;
    }

    public void setFailes(List<ScmUpdateConfPropsResult> failes) {
        this.failes = failes;
    }
}
