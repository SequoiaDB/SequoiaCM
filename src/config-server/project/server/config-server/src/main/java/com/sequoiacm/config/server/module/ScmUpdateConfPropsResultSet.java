package com.sequoiacm.config.server.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScmUpdateConfPropsResultSet {
    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_SET_SUCCESS)
    private List<ScmUpdateConfPropsResult> successes = new ArrayList<>();

    // @JsonProperty(ScmRestArgDefine.CONF_PROPS_RES_SET_FAILES)
    private List<ScmUpdateConfPropsResult> failes = new ArrayList<>();

    private Set<String> rebootConf = new HashSet<>();
    private Map<String, String> adjustConf = new HashMap<>();

    public void addResult(ScmUpdateConfPropsResult result) {
        if (!result.isSuccess()) {
            failes.add(result);
        }
        else {
            successes.add(result);
            rebootConf.addAll(result.getRebootConf());
            adjustConf.putAll(result.getAdjustConf());
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

    public Set<String> getRebootConf() {
        return rebootConf;
    }

    public Map<String, String> getAdjustConf() {
        return adjustConf;
    }

    @Override
    public String toString() {
        return "ScmUpdateConfPropsResultSet{" + "successes=" + successes + ", failes=" + failes
                + ", rebootConf=" + rebootConf + ", adjustConf=" + adjustConf + '}';
    }
}
