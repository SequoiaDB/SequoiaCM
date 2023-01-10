package com.sequoiacm.om.omserver.module.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;

public class OmTransitionBasic {

    @JsonProperty("name")
    private String name;

    @JsonProperty("customized")
    private boolean isCustomized;

    @JsonProperty("source")
    private String source;

    @JsonProperty("dest")
    private String dest;

    @JsonProperty("transition_triggers")
    private OmTransitionTriggers transitionTriggers;

    @JsonProperty("clean_triggers")
    private OmCleanTriggers cleanTriggers;

    @JsonProperty("matcher")
    private String matcher;

    @JsonProperty("is_quick_start")
    private boolean isQuickStart;

    @JsonProperty("is_recycle_space")
    private boolean isRecycleSpace;

    @JsonProperty("data_check_level")
    private String dataCheckLevel;

    @JsonProperty("scope")
    private String scope;

    public OmTransitionBasic() {

    }

    public OmTransitionBasic(String name) {
        this.name = name;
        this.isCustomized = false;
    }

    public OmTransitionBasic(ScmLifeCycleTransition transition) {
        this.name = transition.getName();
        this.source = transition.getSource();
        this.dest = transition.getDest();
        this.matcher = transition.getMatcher();
        this.transitionTriggers = new OmTransitionTriggers(transition.getTransitionTriggers());
        if (transition.getCleanTriggers() != null) {
            this.cleanTriggers = new OmCleanTriggers(transition.getCleanTriggers());
        }
        this.scope = transition.getScope();
        this.isRecycleSpace = transition.isRecycleSpace();
        this.isQuickStart = transition.isQuickStart();
        this.dataCheckLevel = transition.getDataCheckLevel();
    }

    public ScmLifeCycleTransition transformToScmLifeCycleTransition() {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        transition.setName(name);
        transition.setSource(source);
        transition.setDest(dest);
        transition.setMatcher(matcher);
        transition.setTransitionTriggers(transitionTriggers.transformToScmTransitionTriggers());
        if (cleanTriggers != null) {
            transition.setCleanTriggers(cleanTriggers.transformToScmCleanTriggers());
        }
        transition.setScope(scope);
        transition.setQuickStart(isQuickStart);
        transition.setRecycleSpace(isRecycleSpace);
        transition.setDataCheckLevel(dataCheckLevel);
        return transition;
    }

    public String getName() {
        return name;
    }

    public boolean isCustomized() {
        return isCustomized;
    }

    public String getSource() {
        return source;
    }

    public String getDest() {
        return dest;
    }

    public OmTransitionTriggers getTransitionTriggers() {
        return transitionTriggers;
    }

    public OmCleanTriggers getCleanTriggers() {
        return cleanTriggers;
    }

    public String getMatcher() {
        return matcher;
    }

    public boolean isQuickStart() {
        return isQuickStart;
    }

    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    public String getDataCheckLevel() {
        return dataCheckLevel;
    }

    public String getScope() {
        return scope;
    }
}
