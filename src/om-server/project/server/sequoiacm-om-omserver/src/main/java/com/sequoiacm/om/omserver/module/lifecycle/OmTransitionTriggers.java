package com.sequoiacm.om.omserver.module.lifecycle;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmTransitionTriggers;
import com.sequoiacm.client.element.lifecycle.ScmTrigger;

public class OmTransitionTriggers {

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("max_exec_time")
    private long maxExecTime;

    @JsonProperty("rule")
    private String rule;

    @JsonProperty("triggers")
    private List<OmTransitionTrigger> triggers;

    public OmTransitionTriggers() {

    }

    public OmTransitionTriggers(ScmTransitionTriggers transitionTriggers) {
        this.mode = transitionTriggers.getMode();
        this.maxExecTime = transitionTriggers.getMaxExecTime();
        this.rule = transitionTriggers.getRule();
        triggers = new ArrayList<>();
        for (ScmTrigger scmTrigger : transitionTriggers.getTriggerList()) {
            triggers.add(new OmTransitionTrigger(scmTrigger));
        }
    }

    public ScmTransitionTriggers transformToScmTransitionTriggers() {
        ScmTransitionTriggers scmTransitionTriggers = new ScmTransitionTriggers();
        scmTransitionTriggers.setMode(mode);
        scmTransitionTriggers.setRule(rule);
        // 客户端填写单位为 s，此处需转换算为 ms
        scmTransitionTriggers.setMaxExecTime(maxExecTime * 1000);
        List<ScmTrigger> scmTriggers = new ArrayList<>();
        if (triggers != null && triggers.size() > 0) {
            for (OmTransitionTrigger trigger : triggers) {
                scmTriggers.add(trigger.transformToScmTrigger());
            }
        }
        scmTransitionTriggers.setTriggerList(scmTriggers);
        return scmTransitionTriggers;
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
