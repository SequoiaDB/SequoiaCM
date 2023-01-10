package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmCleanTriggers;
import com.sequoiacm.client.element.lifecycle.ScmTrigger;

import java.util.ArrayList;
import java.util.List;

public class OmCleanTriggers {

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("max_exec_time")
    private long maxExecTime;

    @JsonProperty("rule")
    private String rule;

    @JsonProperty("triggers")
    private List<OmTransitionTrigger> triggers;

    public OmCleanTriggers() {

    }

    public OmCleanTriggers(ScmCleanTriggers cleanTriggers) {
        this.mode = cleanTriggers.getMode();
        this.maxExecTime = cleanTriggers.getMaxExecTime();
        this.rule = cleanTriggers.getRule();
        triggers = new ArrayList<>();
        for (ScmTrigger scmTrigger : cleanTriggers.getTriggerList()) {
            triggers.add(new OmTransitionTrigger(scmTrigger));
        }
    }

    public ScmCleanTriggers transformToScmCleanTriggers() {
        ScmCleanTriggers scmCleanTriggers = new ScmCleanTriggers();
        scmCleanTriggers.setMode(mode);
        scmCleanTriggers.setRule(rule);
        // 客户端填写单位为 s，此处需转换算为 ms
        scmCleanTriggers.setMaxExecTime(maxExecTime * 1000);
        List<ScmTrigger> scmTriggers = new ArrayList<>();
        if (triggers != null && triggers.size() > 0) {
            for (OmTransitionTrigger trigger : triggers) {
                scmTriggers.add(trigger.transformToScmTrigger());
            }
        }
        scmCleanTriggers.setTriggerList(scmTriggers);
        return scmCleanTriggers;
    }

    public String getMode() {
        return mode;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public String getRule() {
        return rule;
    }

    public List<OmTransitionTrigger> getTriggers() {
        return triggers;
    }
}
